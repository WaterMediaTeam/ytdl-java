package com.github.kiulian.downloader.cipher;


class SpliceFunction implements CipherFunction {

    @Override
    public char[] apply(final char[] array, final String argument) {
        final int deleteCount = Integer.parseInt(argument);
        final char[] spliced = new char[array.length - deleteCount];
        System.arraycopy(array, 0, spliced, 0, deleteCount);
        System.arraycopy(array, deleteCount * 2, spliced, deleteCount, spliced.length - deleteCount);

        return spliced;
    }

}
