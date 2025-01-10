package com.github.kiulian.downloader.cipher;


import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CachedCipherFactory implements CipherFactory {

    private static final String[] INITIAL_FUNCTION_PATTERNS = new String[]{
            "\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)",
            "([a-zA-Z0-9$]+)\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)",
            "([\"'])signature\\1\\s*,\\s*([a-zA-Z0-9$]+)\\(",
            "\\.sig\\|\\|([a-zA-Z0-9$]+)\\(",
            "yt\\.akamaized\\.net/\\)\\s*\\|\\|\\s*.*?\\s*[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?\\s*()$",
            "\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\(",
            "\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\(",
            "\\bc\\s*&&\\s*a\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "\\bc\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\("
    };

    private static final String FUNCTION_REVERSE_PATTERN = "\\{\\w\\.reverse\\(\\)\\}";
    private static final String FUNCTION_SPLICE_PATTERN = "\\{\\w\\.splice\\(0,\\w\\)\\}";
    private static final String FUNCTION_SWAP1_PATTERN = "\\{var\\s\\w=\\w\\[0];\\w\\[0]=\\w\\[\\w%\\w.length];\\w\\[\\w]=\\w\\}";
    private static final String FUNCTION_SWAP2_PATTERN = "\\{var\\s\\w=\\w\\[0];\\w\\[0]=\\w\\[\\w%\\w.length];\\w\\[\\w%\\w.length]=\\w\\}";
    private static final String FUNCTION_SWAP3_PATTERN = "function\\(\\w+,\\w+\\)\\{var\\s\\w=\\w\\[0];\\w\\[0]=\\w\\[\\w%\\w.length];\\w\\[\\w%\\w.length]=\\w\\}";

    private static final Pattern[] JS_FUNCTION_PATTERNS = new Pattern[]{
            Pattern.compile("\\w+\\.(\\w+)\\(\\w,(\\d+)\\)"),
            Pattern.compile("\\w+\\[(\\\"\\w+\\\")\\]\\(\\w,(\\d+)\\)")
    };

    private final Downloader downloader;

    private final List<Pattern> knownInitialFunctionPatterns = new ArrayList<>();
    private final Map<Pattern, CipherFunction> functionsEquivalentMap = new HashMap<>();
    private final Map<String, Cipher> ciphers = new HashMap<>();

    public CachedCipherFactory(final Downloader downloader) {
        this.downloader = downloader;

        for (final String pattern : INITIAL_FUNCTION_PATTERNS) {
            this.addInitialFunctionPattern(this.knownInitialFunctionPatterns.size(), pattern);
        }

        this.addFunctionEquivalent(FUNCTION_REVERSE_PATTERN, new ReverseFunction());
        this.addFunctionEquivalent(FUNCTION_SPLICE_PATTERN, new SpliceFunction());
        this.addFunctionEquivalent(FUNCTION_SWAP1_PATTERN, new SwapFunctionV1());

        final SwapFunctionV2 swapFunctionV2 = new SwapFunctionV2();
        this.addFunctionEquivalent(FUNCTION_SWAP2_PATTERN, swapFunctionV2);
        this.addFunctionEquivalent(FUNCTION_SWAP3_PATTERN, swapFunctionV2);
    }

    @Override
    public void addInitialFunctionPattern(final int priority, final String regex) {
        this.knownInitialFunctionPatterns.add(priority, Pattern.compile(regex));
    }

    @Override
    public void addFunctionEquivalent(final String regex, final CipherFunction function) {
        this.functionsEquivalentMap.put(Pattern.compile(regex), function);
    }

    @Override
    public Cipher createCipher(final String jsUrl) throws YoutubeException {
        Cipher cipher = this.ciphers.get(jsUrl);

        if (cipher == null) {
            final Response<String> response = this.downloader.downloadWebpage(new RequestWebpage(jsUrl));
            if (!response.ok()) {
                throw new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", jsUrl, response.error().getMessage()));
            }
            final String js = response.data();

            final List<JsFunction> transformFunctions = this.getTransformFunctions(js);
            final String var = transformFunctions.get(0).getVar();

            final String[] transformObject = this.getTransformObject(var, js);
            final Map<String, CipherFunction> transformFunctionsMap = this.getTransformFunctionsMap(transformObject);

            cipher = new DefaultCipher(transformFunctions, transformFunctionsMap);
            this.ciphers.put(jsUrl, cipher);
        }

        return cipher;
    }

    public void clearCache() {
        this.ciphers.clear();
    }

    /**
     * Extract the list of "transform" JavaScript functions calls that
     * the ciphered signature is run through to obtain the actual signature.
     * <p>
     * Example of "transform" functions:
     * Mx.FH(a,11)
     * Mx["do"](a,3)
     * Mx.kT(a,51)
     *
     * @param js The content of the base.js file.
     * @return list of transform functions for deciphering
     * @throws YoutubeException if list of functions could not be found
     */
    private List<JsFunction> getTransformFunctions(final String js) throws YoutubeException {
        final String name = this.getInitialFunctionName(js).replaceAll("[^$A-Za-z0-9_]", "");

        final Pattern pattern = Pattern.compile(Pattern.quote(name) + "=function\\(\\w\\)\\{[a-z=\\.\\(\\\"\\)]*;(.*);(?:.+)\\}");

        final Matcher matcher = pattern.matcher(js);
        if (matcher.find()) {
            final String[] jsFunctions = matcher.group(1).split(";");
            final List<JsFunction> transformFunctions = new ArrayList<>(jsFunctions.length);
            for (final String jsFunction : jsFunctions) {
                final JsFunction parsedFunction = this.parseFunction(jsFunction);
                transformFunctions.add(parsedFunction);
            }
            return transformFunctions;
        }
        throw new YoutubeException.CipherException("Transformation functions not found");
    }

    /**
     * Extract the JsFunction object from JavaScript function call string
     * <p>
     * Example:
     * given function call string as "Mx.FH(a,11)"
     * - object "var" would be "Mx"
     * - function "name" would be "FH"
     * - function "argument" would be "11"
     *
     * @param jsFunction JavaScript function call string
     * @return JsFunction object which represents JavaScript function call
     * @throws YoutubeException if could not parse JavaScript function call
     */
    private JsFunction parseFunction(final String jsFunction) throws YoutubeException {
        for (final Pattern jsFunctionPattern : JS_FUNCTION_PATTERNS) {
            final Matcher matcher = jsFunctionPattern.matcher(jsFunction);

            if (matcher.find()) {
                final String var;
                String[] split = jsFunction.split("\\."); // case: Mx.FH(a,21)
                if (split.length > 1) {
                    var = split[0];
                } else {
                    split = jsFunction.split("\\["); // case: Mx["do"](a,21)
                    if (split.length > 1) {
                        var = split[0];
                    } else {
                        continue;
                    }
                }
                final String name = matcher.group(1);
                final String argument = matcher.group(2);
                return new JsFunction(var, name, argument);
            }
        }
        throw new YoutubeException.CipherException("Could not parse js function");
    }

    /**
     * Extract the name of the function responsible for deciphering the signature
     * based on list of known initial function patterns {@code knownInitialFunctionPatterns}
     *
     * @param js The content of the base.js file.
     * @return initial function name
     * @throws YoutubeException if none of known patterns matches
     */
    private String getInitialFunctionName(final String js) throws YoutubeException {
        for (final Pattern pattern : this.knownInitialFunctionPatterns) {
            final Matcher matcher = pattern.matcher(js);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        throw new YoutubeException.CipherException("Initial function name not found");
    }

    /**
     * Extract the function definitions – "transform object" referenced in the
     * list of transform functions.
     * <p>
     * Example of "transform object":
     * var Mx={
     * FH:function(a){a.reverse()},
     * "do":function(a,b){var c=a[0];a[0]=a[b%a.length];a[b%a.length]=c},
     * xK:function(a,b){a.splice(0,b)}
     * };
     *
     * @param var The obfuscated variable name that stores functions definitions
     *            for deciphering the signature.
     * @param js  The content of the base.js file.
     * @return array of functions definitions for deciphering
     * @throws YoutubeException if "transform object" not found
     */
    private String[] getTransformObject(String var, final String js) throws YoutubeException {
        var = var.replaceAll("[^$A-Za-z0-9_]", "");
        var = Pattern.quote(var);
        final Pattern pattern = Pattern.compile(String.format("var %s=\\{(.*?)\\};", var), Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(js);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\n", " ").split(", ");
        }

        throw new YoutubeException.CipherException("Transform object not found");
    }

    /**
     * Create a map of obfuscated JavaScript function names to the Java equivalents
     *
     * @param transformObject The list of function definitions for deciphering
     * @return map of JS functions to Java equivalents
     * @throws YoutubeException if map function not found
     */
    private Map<String, CipherFunction> getTransformFunctionsMap(final String[] transformObject) throws YoutubeException {
        final Map<String, CipherFunction> mapper = new HashMap<>();
        for (final String obj : transformObject) {
            final String[] split = obj.split(":", 2);
            final String name = split[0];
            final String jsFunction = split[1];

            final CipherFunction function = this.mapFunction(jsFunction);
            mapper.put(name, function);
        }
        return mapper;
    }

    /**
     * For a given JavaScript transform function definition, find the Java equivalent
     *
     * @param jsFunction JavaScript function definition
     * @return Java equivalent for JavaScript transform function
     * @throws YoutubeException if map function not found
     */
    private CipherFunction mapFunction(final String jsFunction) throws YoutubeException {
        for (final Map.Entry<Pattern, CipherFunction> entry : this.functionsEquivalentMap.entrySet()) {
            final Matcher matcher = entry.getKey().matcher(jsFunction);
            if (matcher.find()) {
                return entry.getValue();
            }
        }
        throw new YoutubeException.CipherException("Map function not found");
    }

}