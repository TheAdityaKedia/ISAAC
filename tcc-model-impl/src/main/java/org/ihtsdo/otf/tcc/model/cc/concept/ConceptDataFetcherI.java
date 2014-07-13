package org.ihtsdo.otf.tcc.model.cc.concept;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;


public interface ConceptDataFetcherI {

    byte[] getReadOnlyBytes() throws IOException;

    byte[] getMutableBytes() throws IOException;

    DataInputStream getReadOnlyDataStream() throws IOException;

    DataInputStream getMutableInputStream() throws IOException;

    boolean isPrimordial() throws IOException;

    void reset();
}