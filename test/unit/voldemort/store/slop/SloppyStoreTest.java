/*
 * Copyright 2008-2009 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package voldemort.store.slop;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import voldemort.TestUtils;
import voldemort.store.AbstractByteArrayStoreTest;
import voldemort.store.FailingStore;
import voldemort.store.Store;
import voldemort.store.UnreachableStoreException;
import voldemort.store.memory.InMemoryStorageEngine;
import voldemort.utils.ByteArray;
import voldemort.versioning.Versioned;

public class SloppyStoreTest extends AbstractByteArrayStoreTest {

    private static final byte[] testVal = "test".getBytes();
    private static final int NODE_ID = 0;

    @Override
    @SuppressWarnings("unchecked")
    public Store<ByteArray, byte[], byte[]> getStore() {
        Collection<InMemoryStorageEngine<ByteArray, Slop, byte[]>> backups = Arrays.asList(new InMemoryStorageEngine<ByteArray, Slop, byte[]>("test"));
        return new SloppyStore(NODE_ID,
                               new InMemoryStorageEngine<ByteArray, byte[], byte[]>("test"),
                               backups);
    }

    @SuppressWarnings("unchecked")
    public SloppyStore getSloppyStore(Store<ByteArray, byte[], byte[]> store) {
        Collection<InMemoryStorageEngine<ByteArray, Slop, byte[]>> backups = Arrays.asList(new InMemoryStorageEngine<ByteArray, Slop, byte[]>("test"));
        return new SloppyStore(NODE_ID, store, backups);
    }

    private void assertBackupHasOperation(Slop slop, List<Store<ByteArray, Slop, byte[]>> backups) {
        for(Store<ByteArray, Slop, byte[]> backup: backups) {
            List<Versioned<Slop>> slops = backup.get(slop.makeKey(), slop.getTransforms());
            for(Versioned<Slop> found: slops) {
                Slop foundSlop = found.getValue();
                if(foundSlop.getKey().equals(slop.getKey())
                   && TestUtils.bytesEqual(foundSlop.getValue(), slop.getValue())
                   && foundSlop.getOperation().equals(slop.getOperation()))
                    return;
            }
        }
        fail("Could not find slop " + slop + " in backup stores.");
    }

    public void testFailingStore() {
        SloppyStore store = getSloppyStore(new FailingStore<ByteArray, byte[], byte[]>("test",
                                                                                       new UnreachableStoreException("Unreachable store.")));
        try {
            store.put(new ByteArray(testVal), new Versioned<byte[]>(testVal), null);
            fail("Failing store doesn't fail.");
        } catch(UnreachableStoreException e) {
            Slop slop = new Slop("test", Slop.Operation.PUT, testVal, testVal, NODE_ID, new Date());
            assertBackupHasOperation(slop, store.getBackupStores());
        }
    }

}
