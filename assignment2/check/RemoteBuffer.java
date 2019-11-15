public interface RemoteBuffer extends java.rmi.Remote {
  public StringBuffer buffer() throws java.rmi.RemoteException;
  public void append_R(RemoteBuffer r) throws java.rmi.RemoteException;
  public void append_S(StringBuffer s) throws java.rmi.RemoteException;
}
