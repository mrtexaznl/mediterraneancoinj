/**
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mediterraneancoin.core;

<<<<<<< HEAD:core/src/test/java/com/google/mediterraneancoin/core/AddressTest.java
import com.google.mediterraneancoin.core.Address;
import com.google.mediterraneancoin.core.AddressFormatException;
import com.google.mediterraneancoin.core.NetworkParameters;
import com.google.mediterraneancoin.core.Utils;
import com.google.mediterraneancoin.core.WrongNetworkException;
import com.google.mediterraneancoin.params.MainNetParams;
import com.google.mediterraneancoin.params.TestNet3Params;
import com.google.mediterraneancoin.script.ScriptBuilder;
=======
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
>>>>>>> upstream/master:core/src/test/java/com/google/bitcoin/core/AddressTest.java
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.bitcoin.core.Utils.HEX;
import static org.junit.Assert.*;

public class AddressTest {
    static final NetworkParameters testParams = TestNet3Params.get();
    static final NetworkParameters mainParams = MainNetParams.get();

    @Test
    public void stringification() throws Exception {
        // Test a testnet address.
<<<<<<< HEAD:core/src/test/java/com/google/mediterraneancoin/core/AddressTest.java
        /*
        Address a = new Address(testParams, Hex.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
=======
        Address a = new Address(testParams, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
>>>>>>> upstream/master:core/src/test/java/com/google/bitcoin/core/AddressTest.java
        assertEquals("n4eA2nbYqErp7H6jebchxAN59DmNpksexv", a.toString());
        assertFalse(a.isP2SHAddress());
        */

<<<<<<< HEAD:core/src/test/java/com/google/mediterraneancoin/core/AddressTest.java
        Address b = new Address(mainParams, Hex.decode("13d2261c348202c4ec65cf0d90edeb58bb8a30e9"));
        
        /*
        System.out.println(b.toString());
        
        Address c = new Address(mainParams, "MZ3Zoam3CkaEKGLLVc63PN7S7bnUmVdqs7");
        
        
        String str = new String ( Hex.encode( c.getHash160() ) );
        
        System.out.println( str );
        */
        
        assertEquals("MZ3Zoam3CkaEKGLLVc63PN7S7bnUmVdqs7", b.toString());
=======
        Address b = new Address(mainParams, HEX.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));
        assertEquals("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL", b.toString());
>>>>>>> upstream/master:core/src/test/java/com/google/bitcoin/core/AddressTest.java
        assertFalse(b.isP2SHAddress());
    }
    
    @Test
    public void decoding() throws Exception {
        /*
        Address a = new Address(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
<<<<<<< HEAD:core/src/test/java/com/google/mediterraneancoin/core/AddressTest.java
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", Utils.bytesToHexString(a.getHash160()));
        */

        Address b = new Address(mainParams, "MZ3Zoam3CkaEKGLLVc63PN7S7bnUmVdqs7");
        assertEquals("13d2261c348202c4ec65cf0d90edeb58bb8a30e9", Utils.bytesToHexString(b.getHash160()));
=======
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", Utils.HEX.encode(a.getHash160()));

        Address b = new Address(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals("4a22c3c4cbb31e4d03b15550636762bda0baf85a", Utils.HEX.encode(b.getHash160()));
>>>>>>> upstream/master:core/src/test/java/com/google/bitcoin/core/AddressTest.java
    }
    
    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            new Address(testParams, "this is not a valid address!");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            new Address(testParams, "");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            new Address(testParams, "MZ3Zoam3CkaEKGLLVc63PN7S7bnUmVdqs7");
            fail();
        } catch (WrongNetworkException e) {
            // Success.
            assertEquals(e.verCode, MainNetParams.get().getAddressHeader());
            assertTrue(Arrays.equals(e.acceptableVersions, TestNet3Params.get().getAcceptableAddressCodes()));
        } catch (AddressFormatException e) {
            fail();
        }
    }
    
    @Test
    public void getNetwork() throws Exception {
        NetworkParameters params = Address.getParametersFromAddress("MZ3Zoam3CkaEKGLLVc63PN7S7bnUmVdqs7");
        
        assertEquals(MainNetParams.get().getId(), params.getId());
        
        params = Address.getParametersFromAddress("n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals(TestNet3Params.get().getId(), params.getId());
    }
    
    @Test
    public void p2shAddress() throws Exception {
        // Test that we can construct P2SH addresses
        Address mainNetP2SHAddress = new Address(MainNetParams.get(), "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(mainNetP2SHAddress.version, MainNetParams.get().p2shHeader);
        assertTrue(mainNetP2SHAddress.isP2SHAddress());
        Address testNetP2SHAddress = new Address(TestNet3Params.get(), "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(testNetP2SHAddress.version, TestNet3Params.get().p2shHeader);
        assertTrue(testNetP2SHAddress.isP2SHAddress());

        // Test that we can determine what network a P2SH address belongs to
        NetworkParameters mainNetParams = Address.getParametersFromAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(MainNetParams.get().getId(), mainNetParams.getId());
        NetworkParameters testNetParams = Address.getParametersFromAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(TestNet3Params.get().getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("2ac4b0b501117cc8119c5797b519538d4942e90e");
        Address a = Address.fromP2SHHash(mainParams, hex);
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", a.toString());
        Address b = Address.fromP2SHHash(testParams, HEX.decode("18a0e827269b5211eb51a4af1b2fa69333efa722"));
        assertEquals("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe", b.toString());
        Address c = Address.fromP2SHScript(mainParams, ScriptBuilder.createP2SHOutputScript(hex));
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", c.toString());
    }

    @Test
    public void p2shAddressCreationFromKeys() throws Exception {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        ECKey key1 = new DumpedPrivateKey(mainParams, "5JaTXbAUmfPYZFRwrYaALK48fN6sFJp4rHqq2QSXs8ucfpE4yQU").getKey();
        key1 = ECKey.fromPrivate(key1.getPrivKeyBytes());
        ECKey key2 = new DumpedPrivateKey(mainParams, "5Jb7fCeh1Wtm4yBBg3q3XbT6B525i17kVhy3vMC9AqfR6FH2qGk").getKey();
        key2 = ECKey.fromPrivate(key2.getPrivKeyBytes());
        ECKey key3 = new DumpedPrivateKey(mainParams, "5JFjmGo5Fww9p8gvx48qBYDJNAzR9pmH5S389axMtDyPT8ddqmw").getKey();
        key3 = ECKey.fromPrivate(key3.getPrivKeyBytes());

        List<ECKey> keys = Arrays.asList(key1, key2, key3);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(2, keys);
        Address address = Address.fromP2SHScript(mainParams, p2shScript);
        assertEquals("3N25saC4dT24RphDAwLtD8LUN4E2gZPJke", address.toString());
    }
}
