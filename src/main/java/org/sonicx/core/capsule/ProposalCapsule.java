package org.sonicx.core.capsule;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.StringUtil;
import org.sonicx.core.config.Parameter.ChainConstant;
import org.sonicx.protos.Protocol.Proposal;
import org.sonicx.protos.Protocol.Proposal.State;

@Slf4j(topic = "capsule")
public class ProposalCapsule implements ProtoCapsule<Proposal> {

  private Proposal proposal;

  public ProposalCapsule(final Proposal proposal) {
    this.proposal = proposal;
  }

  public ProposalCapsule(final byte[] data) {
    try {
      this.proposal = Proposal.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public ProposalCapsule(ByteString address, final long id) {
    this.proposal = Proposal.newBuilder()
        .setProposerAddress(address)
        .setProposalId(id)
        .build();
  }

  public long getID() {
    return this.proposal.getProposalId();
  }

  public void setID(long id) {
    this.proposal = this.proposal.toBuilder()
        .setProposalId(id)
        .build();
  }

  public ByteString getProposalAddress() {
    return this.proposal.getProposerAddress();
  }

  public void setProposalAddress(ByteString address) {
    this.proposal = this.proposal.toBuilder()
        .setProposerAddress(address)
        .build();
  }

  public Map<Long, Long> getParameters() {
    return this.proposal.getParametersMap();
  }

  public void setParameters(Map<Long, Long> parameters) {
    this.proposal = this.proposal.toBuilder()
        .putAllParameters(parameters)
        .build();
  }

  public long getExpirationTime() {
    return this.proposal.getExpirationTime();
  }

  public void setExpirationTime(long time) {
    this.proposal = this.proposal.toBuilder()
        .setExpirationTime(time)
        .build();
  }

  public long getCreateTime() {
    return this.proposal.getCreateTime();
  }

  public void setCreateTime(long time) {
    this.proposal = this.proposal.toBuilder()
        .setCreateTime(time)
        .build();
  }

  public List<ByteString> getApprovals() {
    return this.proposal.getApprovalsList();
  }

  public void removeApproval(ByteString address) {
    List<ByteString> approvals = Lists.newArrayList();
    approvals.addAll(getApprovals());
    approvals.remove(address);
    this.proposal = this.proposal.toBuilder()
        .clearApprovals()
        .addAllApprovals(approvals)
        .build();
  }

  public void clearApproval() {
    this.proposal = this.proposal.toBuilder().clearApprovals().build();
  }

  public void addApproval(ByteString committeeAddress) {
    this.proposal = this.proposal.toBuilder()
        .addApprovals(committeeAddress)
        .build();
  }

  public State getState() {
    return this.proposal.getState();
  }

  public void setState(State state) {
    this.proposal = this.proposal.toBuilder()
        .setState(state)
        .build();
  }

  public boolean hasProcessed() {
    return this.proposal.getState().equals(State.DISAPPROVED) || this.proposal.getState()
        .equals(State.APPROVED);
  }

  public boolean hasCanceled() {
    return this.proposal.getState().equals(State.CANCELED);
  }

  public boolean hasExpired(long time) {
    return this.proposal.getExpirationTime() <= time;
  }


  public byte[] createDbKey() {
    return calculateDbKey(getID());
  }

  public static byte[] calculateDbKey(long number) {
    return ByteArray.fromLong(number);
  }

  @Override
  public byte[] getData() {
    return this.proposal.toByteArray();
  }

  @Override
  public Proposal getInstance() {
    return this.proposal;
  }

  public boolean hasMostApprovals(List<ByteString> activeWitnesses) {
    long count = this.proposal.getApprovalsList().stream()
        .filter(witness -> activeWitnesses.contains(witness)).count();
    if (count != this.proposal.getApprovalsCount()) {
      List<ByteString> InvalidApprovalList = this.proposal.getApprovalsList().stream()
          .filter(witness -> !activeWitnesses.contains(witness)).collect(Collectors.toList());
      logger.info("InvalidApprovalList:" + StringUtil.getAddressStringList(InvalidApprovalList));
    }
    if (activeWitnesses.size() != ChainConstant.MAX_ACTIVE_WITNESS_NUM) {
      logger.info("activeWitnesses size = {}", activeWitnesses.size());
    }
	// 2020.8.29: It has been changed from 70% to 60%
    return count >= activeWitnesses.size() * 6 / 10;
  }
}
