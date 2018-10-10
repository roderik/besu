package net.consensys.pantheon.ethereum.vm.operations;

import net.consensys.pantheon.ethereum.core.Gas;
import net.consensys.pantheon.ethereum.vm.AbstractOperation;
import net.consensys.pantheon.ethereum.vm.Code;
import net.consensys.pantheon.ethereum.vm.EVM;
import net.consensys.pantheon.ethereum.vm.ExceptionalHaltReason;
import net.consensys.pantheon.ethereum.vm.GasCalculator;
import net.consensys.pantheon.ethereum.vm.MessageFrame;
import net.consensys.pantheon.util.bytes.Bytes32;
import net.consensys.pantheon.util.uint.UInt256;

import java.util.EnumSet;
import java.util.Optional;

public class JumpiOperation extends AbstractOperation {

  public JumpiOperation(final GasCalculator gasCalculator) {
    super(0x57, "JUMPI", 2, 0, true, 1, gasCalculator);
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    return gasCalculator().getHighTierGasCost();
  }

  @Override
  public void execute(final MessageFrame frame) {
    final UInt256 jumpDestination = frame.popStackItem().asUInt256();
    final Bytes32 condition = frame.popStackItem();

    if (!condition.isZero()) {
      frame.setPC(jumpDestination.toInt());
    } else {
      frame.setPC(frame.getPC() + getOpSize());
    }
  }

  @Override
  public Optional<ExceptionalHaltReason> exceptionalHaltCondition(
      final MessageFrame frame,
      final EnumSet<ExceptionalHaltReason> previousReasons,
      final EVM evm) {
    // If condition is zero (false), no jump is will be performed. Therefore skip the test.
    if (frame.getStackItem(1).isZero()) {
      return Optional.empty();
    }

    final Code code = frame.getCode();
    final UInt256 potentialJumpDestination = frame.getStackItem(0).asUInt256();
    return !code.isValidJumpDestination(evm, potentialJumpDestination)
        ? Optional.of(ExceptionalHaltReason.INVALID_JUMP_DESTINATION)
        : Optional.empty();
  }
}
