package org.sonicx.core.services.http;

import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonicx.api.GrpcAPI;
import org.sonicx.api.GrpcAPI.EasyTransferAssetByPrivateMessage;
import org.sonicx.api.GrpcAPI.EasyTransferResponse;
import org.sonicx.api.GrpcAPI.Return.response_code;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.core.Wallet;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.protos.Contract.TransferAssetContract;
import org.sonicx.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class EasyTransferAssetByPrivateServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
    EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
    boolean visible = false;
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      visible = Util.getVisiblePost(input);
      EasyTransferAssetByPrivateMessage.Builder build = EasyTransferAssetByPrivateMessage
          .newBuilder();
      JsonFormat.merge(input, build, visible);
      byte[] privateKey = build.getPrivateKey().toByteArray();
      ECKey ecKey = ECKey.fromPrivate(privateKey);
      byte[] owner = ecKey.getAddress();
      TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(owner));
      builder.setToAddress(build.getToAddress());
      builder.setAssetName(ByteString.copyFrom(build.getAssetId().getBytes()));
      builder.setAmount(build.getAmount());

      TransactionCapsule transactionCapsule;
      transactionCapsule = wallet
          .createTransactionCapsule(builder.build(), ContractType.TransferAssetContract);
      transactionCapsule.sign(privateKey);
      GrpcAPI.Return retur = wallet.broadcastTransaction(transactionCapsule.getInstance());
      responseBuild.setTransaction(transactionCapsule.getInstance());
      responseBuild.setResult(retur);
      response.getWriter().println(Util.printEasyTransferResponse(responseBuild.build(), visible));
    } catch (Exception e) {
      returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      try {
        response.getWriter().println(JsonFormat.printToString(responseBuild.build(), visible));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
      return;
    }
  }
}
