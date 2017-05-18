package com.squareup.sqlbrite;

import android.database.Cursor;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.FlowableOperator;
import io.reactivex.annotations.NonNull;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;

final class QueryToOneOperator<T> implements FlowableOperator<T, SqlBrite.Query> {
  final Function<Cursor, T> mapper;
  boolean emitDefault;
  T defaultValue;

  QueryToOneOperator(Function<Cursor, T> mapper, boolean emitDefault, T defaultValue) {
    this.mapper = mapper;
    this.emitDefault = emitDefault;
    this.defaultValue = defaultValue;
  }

  /**
   * Applies a function to the child Subscriber and returns a new parent Subscriber.
   *
   * @param observer the child Subscriber instance
   * @return the parent Subscriber instance
   * @throws Exception on failure
   */
  @Override public Subscriber<? super SqlBrite.Query> apply(@NonNull final Subscriber<? super T> observer) throws Exception {
    return new Processor<SqlBrite.Query, SqlBrite.Query>() {
        @Override public void onSubscribe(Subscription s) {
            observer.onSubscribe(s);
        }
        @Override public void onNext(SqlBrite.Query query) {
            try {
                boolean emit = false;
                T item = null;
                Cursor cursor = query.run();
                if (cursor != null) {
                    try {
                        if (cursor.moveToNext()) {
                            item = mapper.apply(cursor);
                            emit = true;
                            if (cursor.moveToNext()) {
                                throw new IllegalStateException("Cursor returned more than 1 row");
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
                if (emit) {
                    observer.onNext(item);
                } else if (emitDefault) {
                    observer.onNext(defaultValue);
                }
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(new Throwable(query.toString(), e));
            }
        }
        @Override public void onError(Throwable t) {
            observer.onError(t);
        }
        @Override public void onComplete() {
            observer.onComplete();
        }
        @Override public void subscribe(Subscriber<? super SqlBrite.Query> s) {

        }
    };
  }
}
