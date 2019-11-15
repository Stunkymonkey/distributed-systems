public class StringServer extends java.rmi.server.UnicastRemoteObject implements RemoteBuffer {
  private StringBuffer buffer;
  public StringServer(String s) throws java.rmi.RemoteException {
    super();
    buffer = new StringBuffer(s);
  }
  @Override
  public StringBuffer buffer() {
    return buffer;
  }

  @Override
  public void append_R(RemoteBuffer r) throws java.rmi.RemoteException {
    r.append_S(new StringBuffer("R"));
    buffer.append(r.buffer());
  }

  @Override
  public void append_S(StringBuffer s) throws java.rmi.RemoteException {
    s.append("S");
    buffer.append(s);
  }

  public static void main(String[] args) throws Exception {
    StringServer s1 = new StringServer("A");
    StringServer s2 = new StringServer("B");
    java.rmi.Naming.bind("rmi://localhost/MyBuffer", s1);
    RemoteBuffer rb = (RemoteBuffer) java.rmi.Naming.lookup("rmi://localhost/MyBuffer");
    
    rb.append_S(s2.buffer());
    System.out.println("1: " + s2.buffer());
    rb.append_R(s2);
    System.out.println("2: " + s2.buffer());
    System.out.println("3: " + rb.buffer());
    java.rmi.Naming.unbind("rmi://localhost/MyBuffer");
  }
}


