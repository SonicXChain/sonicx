package org.sonicx.core.db;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.sonicx.core.capsule.TransactionInfoCapsule;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.exception.BadItemException;

@Component
public class TransactionHistoryStore extends SonicxStoreWithRevoking<TransactionInfoCapsule> {

  @Autowired
  public TransactionHistoryStore(@Value("transactionHistoryStore") String dbName) {
    super(dbName);
  }

  @Override
  public TransactionInfoCapsule get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionInfoCapsule(value);
  }

  @Override
  public void put(byte[] key, TransactionInfoCapsule item) {
    if (BooleanUtils.toBoolean(Args.getInstance().getStorage().getTransactionHistoreSwitch())) {
      super.put(key, item);
    }
  }
}