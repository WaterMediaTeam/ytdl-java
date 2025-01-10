package com.github.kiulian.downloader.cipher;


class ReverseFunction implements CipherFunction {

    @Override
    public char[] apply(final char[] array, final String argument) {
        final StringBuilder sb = new StringBuilder().append(array);
        return sb.reverse().toString().toCharArray();
    }

}
