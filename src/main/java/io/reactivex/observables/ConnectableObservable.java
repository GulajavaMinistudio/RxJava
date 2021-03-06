/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.observables;

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.operators.observable.*;
import io.reactivex.internal.util.ConnectConsumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * A {@code ConnectableObservable} resembles an ordinary {@link Observable}, except that it does not begin
 * emitting items when it is subscribed to, but only when its {@link #connect} method is called. In this way you
 * can wait for all intended {@link Observer}s to {@link Observable#subscribe} to the {@code Observable}
 * before the {@code Observable} begins emitting items.
 * <p>
 * <img width="640" height="510" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/publishConnect.png" alt="">
 *
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators">RxJava Wiki:
 *      Connectable Observable Operators</a>
 * @param <T>
 *          the type of items emitted by the {@code ConnectableObservable}
 */
public abstract class ConnectableObservable<T> extends Observable<T> {

    /**
     * Instructs the {@code ConnectableObservable} to begin emitting the items from its underlying
     * {@link Observable} to its {@link Observer}s.
     *
     * @param connection
     *          the action that receives the connection subscription before the subscription to source happens
     *          allowing the caller to synchronously disconnect a synchronous source
     * @see <a href="http://reactivex.io/documentation/operators/connect.html">ReactiveX documentation: Connect</a>
     */
    public abstract void connect(@NonNull Consumer<? super Disposable> connection);

    /**
     * Instructs the {@code ConnectableObservable} to begin emitting the items from its underlying
     * {@link Observable} to its {@link Observer}s.
     * <p>
     * To disconnect from a synchronous source, use the {@link #connect(Consumer)} method.
     *
     * @return the subscription representing the connection
     * @see <a href="http://reactivex.io/documentation/operators/connect.html">ReactiveX documentation: Connect</a>
     */
    public final Disposable connect() {
        ConnectConsumer cc = new ConnectConsumer();
        connect(cc);
        return cc.disposable;
    }

    /**
     * Apply a workaround for a race condition with the regular publish().refCount()
     * so that racing observers and refCount won't hang.
     * 
     * @return the ConnectableObservable to work with
     * @since 2.2.10
     */
    @SuppressWarnings("unchecked")
    private ConnectableObservable<T> onRefCount() {
        if (this instanceof ObservablePublishClassic) {
            return RxJavaPlugins.onAssembly(
                    new ObservablePublishAlt<T>(((ObservablePublishClassic<T>)this).publishSource())
                   );
        }
        return this;
    }

    /**
     * Returns an {@code Observable} that stays connected to this {@code ConnectableObservable} as long as there
     * is at least one subscription to this {@code ConnectableObservable}.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload does not operate on any particular {@link Scheduler}.</dd>
     * </dl>
     * @return an {@link Observable}
     * @see <a href="http://reactivex.io/documentation/operators/refcount.html">ReactiveX documentation: RefCount</a>
     * @see #refCount(int)
     * @see #refCount(long, TimeUnit)
     * @see #refCount(int, long, TimeUnit)
     */
    @NonNull
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public Observable<T> refCount() {
        return RxJavaPlugins.onAssembly(new ObservableRefCount<T>(onRefCount()));
    }

    /**
     * Connects to the upstream {@code ConnectableObservable} if the number of subscribed
     * subscriber reaches the specified count and disconnect if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload does not operate on any particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param subscriberCount the number of subscribers required to connect to the upstream
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Observable<T> refCount(int subscriberCount) {
        return refCount(subscriberCount, 0, TimeUnit.NANOSECONDS, Schedulers.trampoline());
    }

    /**
     * Connects to the upstream {@code ConnectableObservable} if the number of subscribed
     * subscriber reaches 1 and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @return the new Observable instance
     * @see #refCount(long, TimeUnit, Scheduler)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Observable<T> refCount(long timeout, TimeUnit unit) {
        return refCount(1, timeout, unit, Schedulers.computation());
    }

    /**
     * Connects to the upstream {@code ConnectableObservable} if the number of subscribed
     * subscriber reaches 1 and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the specified {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @param scheduler the target scheduler to wait on before disconnecting
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Observable<T> refCount(long timeout, TimeUnit unit, Scheduler scheduler) {
        return refCount(1, timeout, unit, scheduler);
    }

    /**
     * Connects to the upstream {@code ConnectableObservable} if the number of subscribed
     * subscriber reaches the specified count and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param subscriberCount the number of subscribers required to connect to the upstream
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @return the new Observable instance
     * @see #refCount(int, long, TimeUnit, Scheduler)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public final Observable<T> refCount(int subscriberCount, long timeout, TimeUnit unit) {
        return refCount(subscriberCount, timeout, unit, Schedulers.computation());
    }

    /**
     * Connects to the upstream {@code ConnectableObservable} if the number of subscribed
     * subscriber reaches the specified count and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the specified {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param subscriberCount the number of subscribers required to connect to the upstream
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @param scheduler the target scheduler to wait on before disconnecting
     * @return the new Observable instance
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public final Observable<T> refCount(int subscriberCount, long timeout, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.verifyPositive(subscriberCount, "subscriberCount");
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableRefCount<T>(onRefCount(), subscriberCount, timeout, unit, scheduler));
    }

    /**
     * Returns an Observable that automatically connects (at most once) to this ConnectableObservable
     * when the first Observer subscribes.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/autoConnect.o.png" alt="">
     * <p>
     * The connection happens after the first subscription and happens at most once
     * during the lifetime of the returned Observable. If this ConnectableObservable
     * terminates, the connection is never renewed, no matter how Observers come
     * and go. Use {@link #refCount()} to renew a connection or dispose an active
     * connection when all {@code Observer}s have disposed their {@code Disposable}s.
     * <p>
     * This overload does not allow disconnecting the connection established via
     * {@link #connect(Consumer)}. Use the {@link #autoConnect(int, Consumer)} overload
     * to gain access to the {@code Disposable} representing the only connection.
     *
     * @return an Observable that automatically connects to this ConnectableObservable
     *         when the first Observer subscribes
     */
    @NonNull
    public Observable<T> autoConnect() {
        return autoConnect(1);
    }

    /**
     * Returns an Observable that automatically connects (at most once) to this ConnectableObservable
     * when the specified number of Observers subscribe to it.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/autoConnect.o.png" alt="">
     * <p>
     * The connection happens after the given number of subscriptions and happens at most once
     * during the lifetime of the returned Observable. If this ConnectableObservable
     * terminates, the connection is never renewed, no matter how Observers come
     * and go. Use {@link #refCount()} to renew a connection or dispose an active
     * connection when all {@code Observer}s have disposed their {@code Disposable}s.
     * <p>
     * This overload does not allow disconnecting the connection established via
     * {@link #connect(Consumer)}. Use the {@link #autoConnect(int, Consumer)} overload
     * to gain access to the {@code Disposable} representing the only connection.
     *
     * @param numberOfSubscribers the number of subscribers to await before calling connect
     *                            on the ConnectableObservable. A non-positive value indicates
     *                            an immediate connection.
     * @return an Observable that automatically connects to this ConnectableObservable
     *         when the specified number of Subscribers subscribe to it
     */
    @NonNull
    public Observable<T> autoConnect(int numberOfSubscribers) {
        return autoConnect(numberOfSubscribers, Functions.emptyConsumer());
    }

    /**
     * Returns an Observable that automatically connects (at most once) to this ConnectableObservable
     * when the specified number of Subscribers subscribe to it and calls the
     * specified callback with the Subscription associated with the established connection.
     * <p>
     * <img width="640" height="348" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/autoConnect.o.png" alt="">
     * <p>
     * The connection happens after the given number of subscriptions and happens at most once
     * during the lifetime of the returned Observable. If this ConnectableObservable
     * terminates, the connection is never renewed, no matter how Observers come
     * and go. Use {@link #refCount()} to renew a connection or dispose an active
     * connection when all {@code Observer}s have disposed their {@code Disposable}s.
     *
     * @param numberOfSubscribers the number of subscribers to await before calling connect
     *                            on the ConnectableObservable. A non-positive value indicates
     *                            an immediate connection.
     * @param connection the callback Consumer that will receive the Subscription representing the
     *                   established connection
     * @return an Observable that automatically connects to this ConnectableObservable
     *         when the specified number of Subscribers subscribe to it and calls the
     *         specified callback with the Subscription associated with the established connection
     */
    @NonNull
    public Observable<T> autoConnect(int numberOfSubscribers, @NonNull Consumer<? super Disposable> connection) {
        if (numberOfSubscribers <= 0) {
            this.connect(connection);
            return RxJavaPlugins.onAssembly(this);
        }
        return RxJavaPlugins.onAssembly(new ObservableAutoConnect<T>(this, numberOfSubscribers, connection));
    }
}
