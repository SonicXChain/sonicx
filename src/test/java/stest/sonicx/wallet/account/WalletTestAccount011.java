package stest.sonicx.wallet.account;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.sonicx.api.GrpcAPI.AccountResourceMessage;
import org.sonicx.api.GrpcAPI.EmptyMessage;
import org.sonicx.api.WalletGrpc;
import org.sonicx.api.WalletSolidityGrpc;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.Utils;
import org.sonicx.core.Wallet;
import org.sonicx.protos.Protocol.Account;
import stest.sonicx.wallet.common.client.Configuration;
import stest.sonicx.wallet.common.client.Parameter.CommonConstant;
import stest.sonicx.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAccount011 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account011Address = ecKey1.getAddress();
  String account011Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(account011Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

  }

  @Test(enabled = true)
  public void testgenerateAddress() {
    EmptyMessage.Builder builder = EmptyMessage.newBuilder();
    blockingStubFull.generateAddress(builder.build());
    blockingStubSolidity.generateAddress(builder.build());
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);

    }

  }
}
