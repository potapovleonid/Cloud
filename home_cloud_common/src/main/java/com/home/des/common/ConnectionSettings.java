package com.home.des.common;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConnectionSettings {
    public final static int PORT = 8989;
    public final static String HOST = "localhost";
    public final static Path destination_server_files = Paths.get("./server_files/");
}
