package com.squareup.sqlbrite;

import android.database.Cursor;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.FlowableOperator;
import io.reactivex.annotations.NonNull;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;

final class QueryToListOperator<T> implements FlowableOperator<List<T>, SqlBrite.Query> {
  final Function<Cursor, T> mapper;

  QueryToListOperator(Function<Cursor, T> mapper) {
    this.mapper = mapper;
  }

  /**
   * Applies a function to the child Subscriber and returns a new parent Subscriber.
   *
   * @param observer the child Subscriber instance
   * @return the parent Subscriber instance
   * @throws Exception on failure
   */
  @Override public Subscriber<? super SqlBrite.Query> apply(@NonNull final Subscriber<? super List<T>> observer)
          throws Exception {
    return new Processor<SqlBrite.Query, SqlBrite.Query>() {
      @Override public void onSubscribe(Subscription s) {
        observer.onSubscribe(s);
      }

      @Override public void onNext(SqlBrite.Query query) {
        try {
          Cursor cursor = query.run();
          if (cursor == null) {
            return;
          }
          List<T> items = new ArrayList<>(cursor.getCount());
          try {
            while (cursor.moveToNext()) {
              items.add(mapper.apply(cursor));
            }
          } finally {
            cursor.close();
          }
          observer.onNext(items);
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
