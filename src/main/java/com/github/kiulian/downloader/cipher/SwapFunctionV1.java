package com.github.kiulian.downloader.cipher;


class SwapFunctionV1 implements CipherFunction {

    @Override
    public char[] apply(final char[] array, final String argument) {
        final int position = Integer.parseInt(argument);
        final char c = array[0];
        array[0] = array[position % array.length];
        array[position] = c;
        return array;
    }

}
