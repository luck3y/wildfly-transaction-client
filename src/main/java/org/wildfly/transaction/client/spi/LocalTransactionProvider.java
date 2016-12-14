/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.transaction.client.spi;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.wildfly.common.Assert;
import org.wildfly.common.annotation.NotNull;
import org.wildfly.transaction.client.SimpleXid;
import org.wildfly.transaction.client.XAImporter;
import org.wildfly.transaction.client._private.Log;

/**
 * A local transaction provider.  Such a provider must implement all methods on this interface in order for
 * local transactions to be supported.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface LocalTransactionProvider extends TransactionProvider {
    /**
     * Get the transaction manager.
     *
     * @return the transaction manager (must not be {@code null})
     */
    @NotNull
    TransactionManager getTransactionManager();

    /**
     * Get the XA importer.
     *
     * @return the XA importer (must not be {@code null})
     */
    @NotNull
    XAImporter getXAImporter();

    /**
     * Create and start a new local transaction, which is not associated with any particular thread.
     *
     * @param timeout the timeout to use for the new transaction
     * @return the new transaction (must not be {@code null})
     * @throws SystemException if the creation of the transaction failed for some reason
     * @throws SecurityException if the caller is not authorized to create a transaction
     */
    @NotNull
    Transaction createNewTransaction(final int timeout) throws SystemException, SecurityException;

    /**
     * Determine whether the given transaction was imported or originated locally.
     *
     * @param transaction the transaction to test (not {@code null})
     * @return {@code true} if the transaction was imported, or {@code false} if it was created locally
     * @throws IllegalArgumentException if the transaction does not belong to this provider
     */
    boolean isImported(@NotNull Transaction transaction) throws IllegalArgumentException;

    /**
     * Register an interposed synchronization on the given transaction.
     *
     * @param transaction the transaction (not {@code null})
     * @param sync the synchronization (not {@code null})
     * @throws IllegalArgumentException if the transaction does not belong to this provider
     */
    void registerInterposedSynchronization(@NotNull Transaction transaction, @NotNull Synchronization sync) throws IllegalArgumentException;

    /**
     * Get a resource associated with the given transaction.
     *
     * @param transaction the transaction (not {@code null})
     * @param key the key to look up (not {@code null})
     * @return the resource, or {@code null} if none is set
     * @throws IllegalArgumentException if the transaction does not belong to this provider
     */
    Object getResource(@NotNull Transaction transaction, @NotNull Object key);

    /**
     * Put a resource on to the given transaction.
     *
     * @param transaction the transaction (not {@code null})
     * @param key the key to store under (not {@code null})
     * @param value the value to store
     * @throws IllegalArgumentException if the transaction does not belong to this provider
     */
    void putResource(@NotNull Transaction transaction, @NotNull Object key, Object value) throws IllegalArgumentException;

    /**
     * Determine if the given transaction is rollback-only.
     *
     * @param transaction the transaction (not {@code null})
     * @return {@code true} if the transaction is rollback-only, {@code false} otherwise
     * @throws IllegalArgumentException if the transaction does not belong to this provider
     */
    boolean getRollbackOnly(@NotNull Transaction transaction) throws IllegalArgumentException;

    /**
     * Get a key which has the same equals and hashCode behavior as the given transaction.
     *
     * @param transaction the transaction (not {@code null})
     * @return the key object (must not be {@code null})
     * @throws IllegalArgumentException if the transaction does not belong to this provider
     */
    @NotNull Object getKey(@NotNull Transaction transaction) throws IllegalArgumentException;

    /**
     * An empty provider which does not support new transactions.
     */
    LocalTransactionProvider EMPTY = new LocalTransactionProvider() {

        private final TransactionManager transactionManager = new TransactionManager() {
            public void begin() throws NotSupportedException, SystemException {
                throw Assert.unsupported();
            }

            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
                throw Log.log.noTransaction();
            }

            public void rollback() throws IllegalStateException, SecurityException, SystemException {
                throw Log.log.noTransaction();
            }

            public void setRollbackOnly() throws IllegalStateException, SystemException {
                throw Log.log.noTransaction();
            }

            public int getStatus() throws SystemException {
                return Status.STATUS_NO_TRANSACTION;
            }

            public Transaction getTransaction() throws SystemException {
                return null;
            }

            public void setTransactionTimeout(final int seconds) throws SystemException {
                // ignored
            }

            public Transaction suspend() throws SystemException {
                throw Log.log.noTransaction();
            }

            public void resume(final Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
                throw Log.log.transactionNotAssociatedWithThisProvider();
            }
        };
        private final XAImporter xaImporter = new XAImporter() {
            @NotNull
            public ProviderImportResult findOrImportTransaction(final Xid xid, final int timeout) throws XAException {
                throw Assert.unsupported();
            }

            public Transaction findExistingTransaction(final Xid xid) throws XAException {
                return null;
            }

            public void beforeComplete(final Xid xid) throws XAException {
                throw Log.log.noTransactionXa(XAException.XAER_NOTA);
            }

            public void commit(final Xid xid, final boolean onePhase) throws XAException {
                throw Log.log.noTransactionXa(XAException.XAER_NOTA);
            }

            public void forget(final Xid xid) throws XAException {
                throw Log.log.noTransactionXa(XAException.XAER_NOTA);
            }

            public int prepare(final Xid xid) throws XAException {
                throw Log.log.noTransactionXa(XAException.XAER_NOTA);
            }

            public void rollback(final Xid xid) throws XAException {
                throw Log.log.noTransactionXa(XAException.XAER_NOTA);
            }

            @NotNull
            public Xid[] recover(final int flag) throws XAException {
                return SimpleXid.NO_XIDS;
            }
        };

        @NotNull
        public TransactionManager getTransactionManager() {
            return transactionManager;
        }

        @NotNull
        public XAImporter getXAImporter() {
            return xaImporter;
        }

        @NotNull
        public Transaction createNewTransaction(final int timeout) throws SystemException, SecurityException {
            throw Assert.unsupported();
        }

        public boolean isImported(@NotNull final Transaction transaction) throws IllegalArgumentException {
            throw new IllegalArgumentException(Log.log.transactionNotAssociatedWithThisProvider().getMessage());
        }

        public void registerInterposedSynchronization(@NotNull final Transaction transaction, @NotNull final Synchronization sync) throws IllegalArgumentException {
            throw new IllegalArgumentException(Log.log.transactionNotAssociatedWithThisProvider().getMessage());
        }

        public Object getResource(@NotNull final Transaction transaction, @NotNull final Object key) {
            throw new IllegalArgumentException(Log.log.transactionNotAssociatedWithThisProvider().getMessage());
        }

        public void putResource(@NotNull final Transaction transaction, @NotNull final Object key, final Object value) throws IllegalArgumentException {
            throw new IllegalArgumentException(Log.log.transactionNotAssociatedWithThisProvider().getMessage());
        }

        public boolean getRollbackOnly(@NotNull final Transaction transaction) throws IllegalArgumentException {
            throw new IllegalArgumentException(Log.log.transactionNotAssociatedWithThisProvider().getMessage());
        }

        @NotNull
        public Object getKey(@NotNull final Transaction transaction) throws IllegalArgumentException {
            throw new IllegalArgumentException(Log.log.transactionNotAssociatedWithThisProvider().getMessage());
        }
    };
}
