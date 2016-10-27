package cs.bilkent.joker.engine.kvstore;

import cs.bilkent.joker.engine.partition.PartitionKey;
import cs.bilkent.joker.operator.kvstore.KVStore;


public interface KVStoreContext
{

    String getOperatorId ();

    KVStore getKVStore ( PartitionKey key );

}
