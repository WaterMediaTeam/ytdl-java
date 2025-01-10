package com.github.kiulian.downloader.downloader.response;

import java.util.concurrent.*;

public class ResponseImpl<T> implements Response<T> {

    private final Future<T> data;
    private Throwable error;

    private ResponseImpl(final Future<T> data, final Throwable error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ResponseImpl<T> from(final T data) {
        final Future<T> future = new Future<T>() {

            @Override
            public T get(final long timeout, final TimeUnit unit) {
                return this.get();
            }

            @Override
            public T get() {
                return data;
            }

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }
        };

        return fromFuture(future);
    }

    public static <T> ResponseImpl<T> fromFuture(final Future<T> data) {
        return new ResponseImpl<>(data, null);
    }

    public static <T> ResponseImpl<T> error(final Throwable throwable) {
        return new ResponseImpl<>(null, throwable);
    }

    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public T data() {
        if (this.data != null) {
            try {
                return this.data.get();
            } catch (final InterruptedException | ExecutionException e) {
                this.error = e;
            }
        }
        return null;
    }


    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public T data(final long timeout, final TimeUnit unit) throws TimeoutException {
        if (this.data != null) {
            try {
                return this.data.get(timeout, unit);
            } catch (final InterruptedException | ExecutionException e) {
                this.error = e;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public Throwable error() {
        if (this.data != null) {
            try {
                this.data.get();
            } catch (final InterruptedException | ExecutionException e) {
                this.error = e;
                return e;
            }
        }
        return this.error;
    }

    @Override
    public ResponseStatus status() {
        if (this.error != null) {
            return ResponseStatus.error;
        }
        if (this.data != null) {
            if (this.data.isCancelled()) {
                return ResponseStatus.canceled;
            }

            try {
                ((Future<?>) this.data).get(1, TimeUnit.MILLISECONDS);
            } catch (final CancellationException e) {
                return ResponseStatus.canceled;
            } catch (final TimeoutException e) {
                return ResponseStatus.downloading;
            } catch (final ExecutionException | InterruptedException e) {
                this.error = e;
                return ResponseStatus.error;
            }
            return ResponseStatus.completed;
        }
        return ResponseStatus.error;
    }

    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public boolean ok() {
        if (this.error != null) {
            return false;
        }

        try {
            ((Future<?>) this.data).get();
            return true;
        } catch (final CancellationException ignored) {
        } catch (final Exception e) {
            this.error = e;
        }

        return false;
    }

    @Override
    public boolean cancel() {
        if (this.error != null)
            return false;
        return this.data.cancel(true);
    }
}
