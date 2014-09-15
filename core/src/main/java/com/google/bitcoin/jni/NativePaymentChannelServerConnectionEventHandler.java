package com.google.bitcoin.jni;

//import com.google.mediterraneancoin.core.*;
import com.google.mediterraneancoin.core.Coin;
import com.google.mediterraneancoin.core.Sha256Hash;
import com.google.mediterraneancoin.protocols.channels.PaymentChannelCloseException;
import com.google.mediterraneancoin.protocols.channels.ServerConnectionEventHandler;
import com.google.protobuf.ByteString;

import java.math.BigInteger;

/**
 * An event listener that relays events to a native C++ object. A pointer to that object is stored in
 * this class using JNI on the native side, thus several instances of this can point to different actual
 * native implementations.
 */
public class NativePaymentChannelServerConnectionEventHandler extends ServerConnectionEventHandler {
    public long ptr;

    @Override
    public native void channelOpen(Sha256Hash channelId);

    @Override
    public native ByteString paymentIncrease(Coin by, Coin to, ByteString info);

    @Override
    public native void channelClosed(PaymentChannelCloseException.CloseReason reason);
}
