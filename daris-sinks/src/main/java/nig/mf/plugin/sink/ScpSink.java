package nig.mf.plugin.sink;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import arc.archive.ArchiveInput;
import arc.archive.ArchiveRegistry;
import arc.mf.plugin.DataSinkImpl;
import arc.mf.plugin.PluginTask;
import arc.mf.plugin.PluginThread;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.DataType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.PasswordType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.sink.ParameterDefinition;
import arc.mime.NamedMimeType;
import arc.streams.LongInputStream;
import arc.streams.StreamCopy;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import io.github.xtman.ssh.client.Connection;
import io.github.xtman.ssh.client.ConnectionBuilder;
import io.github.xtman.ssh.client.ScpPutClient;
import io.github.xtman.util.PathUtils;
import nig.ssh.client.Ssh;

public class ScpSink implements DataSinkImpl {

    public static final String SINK_TYPE = "scp";

    public static final int DEFAULT_FILE_MODE = 0640;

    public static final int DEFAULT_DIR_MODE = 0755;

    public static enum ParamDefn {

        // @formatter:off
        HOST("host", StringType.DEFAULT, "The address of the retmote SSH server host."), 
        PORT("port", new IntegerType(1, 65535), "The port of the remote SSH server. Defaults to " + Ssh.DEFAULT_PORT), 
        HOST_KEY("host-key",StringType.DEFAULT, "The public key of the remote SSH server host. If specified, it will be used to verify the remote host. If not specified, accepts any remote host key. Note: RSA and DSA encoded public key are supported. However, ECDSA encoded key is NOT supported."),
        USER("user", StringType.DEFAULT, "The user login for accessing the remote SSH server"),
        PASSWORD("password",PasswordType.DEFAULT, "The user password for accessing the remote SSH server. If not specified, private-key must be specified."),
        PRIVATE_KEY("private-key",PasswordType.DEFAULT, "The user's private key for authenticating with the remote SSH server using pubkey authentication. It assumes the user's public key has been installed on the server, e.g. ~/.ssh/authorized_keys. If not specified, password must be specified."),
        PASSPHRASE("passphrase", PasswordType.DEFAULT, "The passphrase for the private-key. It is only required when the private-key argument is given and it is encrypted."),
        DIRECTORY("directory",StringType.DEFAULT, "The base directory on the remote SSH server. If not set, the user's home direcotry will be used."),
        DECOMPRESS("decompress", BooleanType.DEFAULT, "Indicate whether to decompress the archive. Defaults to false. Note: it can only decompress the recognized archive types, zip, tar, gzip, bzip2 and aar. Also if the calling service e.g. shopping cart services already decompress the archive, turning off the decompress for the sink can do nothing but just transfer the decompressed data."),
        FILE_MODE("file-mode", new StringType(Pattern.compile("^[0-7]{4}$")),"The remote file mode. Defaults to "+ String.format("%04o", DEFAULT_FILE_MODE) +"." ),
        DIR_MODE("dir-mode", new StringType(Pattern.compile("^[0-7]{4}$")),"The remote directory mode. Defaults to "+ String.format("%04o", DEFAULT_DIR_MODE) +"." ),
        LAYOUT_PATTERN("layout-pattern", StringType.DEFAULT, "The expression to generate output path.");
        // @formatter:on

        private String _paramName;
        private ParameterDefinition _paramDefn;

        ParamDefn(String name, DataType type, String description) {
            _paramName = name;
            _paramDefn = new ParameterDefinition(type, description);
        }

        @Override
        public final String toString() {
            return paramName();
        }

        public String paramName() {
            return _paramName;
        }

        public ParameterDefinition definition() {
            return _paramDefn;
        }

        private static Map<String, ParameterDefinition> _defns;

        public static Map<String, ParameterDefinition> definitions() {
            if (_defns == null) {
                _defns = new LinkedHashMap<String, ParameterDefinition>();
                ParamDefn[] vs = values();
                for (ParamDefn v : vs) {
                    _defns.put(v.paramName(), v.definition());
                }
            }
            return Collections.unmodifiableMap(_defns);
        }
    }

    private static class Params {
        public final String host;
        public final int port;
        public final String hostKey;
        public final String user;
        public final String password;
        public final String privateKey;
        public final String passphrase;
        public final String directory;
        public final boolean decompress;
        public final int fileMode;
        public final int dirMode;
        public final String layoutPattern;

        Params(String host, int port, String hostKey, String user, String password, String privateKey,
                String passphrase, String directory, boolean decompress, int fileMode, int dirMode,
                String layoutPattern) {
            this.host = host;
            this.port = port;
            this.hostKey = hostKey;
            this.user = user;
            this.password = password;
            this.privateKey = privateKey;
            this.passphrase = passphrase;
            this.directory = directory;
            this.decompress = decompress;
            this.fileMode = fileMode;
            this.dirMode = dirMode;
            this.layoutPattern = layoutPattern;
        }

        public static Params parse(Map<String, String> params) throws Throwable {
            if (params == null || params.isEmpty()) {
                throw new IllegalArgumentException("Sink parameters cannot be null or empty.");
            }

            String host = params.get(ParamDefn.HOST.paramName());
            if (host == null) {
                throw new IllegalArgumentException("Missing sink parameter: " + ParamDefn.HOST.paramName());
            }

            String p = params.get(ParamDefn.PORT.paramName());
            int port = p == null ? Ssh.DEFAULT_PORT : Integer.parseInt(p);

            String hostKey = params.get(ParamDefn.HOST_KEY.paramName());
            // if (hostKey == null) {
            // throw new IllegalArgumentException("Missing sink parameter: " +
            // ParamDefn.HOSTKEY.paramName());
            // }

            String user = params.get(ParamDefn.USER.paramName());
            if (user == null) {
                throw new IllegalArgumentException("Missing sink parameter: " + ParamDefn.USER.paramName());
            }

            String password = params.get(ParamDefn.PASSWORD.paramName());
            String privateKey = params.get(ParamDefn.PRIVATE_KEY.paramName());
            if (password == null && privateKey == null) {
                throw new IllegalArgumentException("Missing sink parameter: " + ParamDefn.PASSWORD.paramName() + " or "
                        + ParamDefn.PRIVATE_KEY.paramName() + ". Expecting at least one. Found none.");
            }
            String passphrase = params.get(ParamDefn.PASSPHRASE.paramName());
            if (passphrase != null && privateKey == null) {
                throw new IllegalArgumentException("passphrase for private-key is given but the private-key is null.");
            }

            String directory = params.get(ParamDefn.DIRECTORY.paramName());
            if (directory != null) {
                directory = PathUtils.normalise(directory.trim());
            }

            String d = params.get(ParamDefn.DECOMPRESS.paramName());
            boolean decompress = d == null ? false : Boolean.parseBoolean(d);

            int fileMode = DEFAULT_FILE_MODE;
            String fileModeStr = params.get(ParamDefn.FILE_MODE.paramName());
            if (fileModeStr != null) {
                fileMode = Integer.parseInt(fileModeStr, 8);
            }

            int dirMode = DEFAULT_DIR_MODE;
            String dirModeStr = params.get(ParamDefn.DIR_MODE.paramName());
            if (dirModeStr != null) {
                dirMode = Integer.parseInt(dirModeStr, 8);
            }

            String layoutPattern = params.get(ParamDefn.LAYOUT_PATTERN.paramName());

            return new Params(host, port, hostKey, user, password, privateKey, passphrase, directory, decompress,
                    fileMode, dirMode, layoutPattern);
        }
    }

    private static ScpPutClient createScpPutClient(Params params) throws Throwable {
        ConnectionBuilder builder = new ConnectionBuilder("jsch");
        builder.setServer(params.host, params.port, params.hostKey);
        if (params.password != null) {
            builder.setUserCredentials(params.user, params.password);
        } else {
            builder.setUserCredentials(params.user, params.privateKey, params.passphrase);
        }
        Connection cxn = builder.build();
        return cxn.createScpPutClient(params.directory, params.dirMode, params.fileMode);
    }

    @Override
    public String[] acceptedTypes() throws Throwable {
        // all mime types are accepted.
        return null;
    }

    @Override
    public Object beginMultiple(Map<String, String> parameters) throws Throwable {

        Params params = Params.parse(parameters);
        return createScpPutClient(params);
    }

    @Override
    public int compressionLevelRequired() {
        // don't care
        return -1;
    }

    @Override
    public void consume(final java.lang.Object multipleTransferContext, java.lang.String path,
            java.util.Map<java.lang.String, java.lang.String> parameters, XmlDoc.Element userMeta, XmlDoc.Element meta,
            LongInputStream in, java.lang.String appMimeType, java.lang.String streamMimeType, long length)
            throws Throwable {
        // @formatter:off
//        System.out.println("path: " + path);
//        System.out.println("userMeta: " + userMeta);
//        System.out.println("meta: " + meta);
//        System.out.println("appMimeType: " + appMimeType);
//        System.out.println("streamMimeType: " + streamMimeType);
//        System.out.println("multi-transfer: " + (multipleTransferContext != null));
        // @formatter:on

        Params params = Params.parse(parameters);
        final ScpPutClient scp = multipleTransferContext == null ? createScpPutClient(params)
                : (ScpPutClient) multipleTransferContext;
        String assetId = meta != null ? meta.value("@id") : null;
        String assetName = meta != null ? meta.value("name") : null;
        String ext = meta != null ? meta.value("content/type/@ext") : null;

        try {
            StringBuilder sb = new StringBuilder();
            if (params.layoutPattern != null && assetId != null) {
                sb.append(generatePath(PluginThread.serviceExecutor(), assetId, params.layoutPattern));
            } else if (path != null) {
                sb.append(PathUtils.normalise(path));
            } else {

            }

            if (params.decompress && streamMimeType != null && ArchiveRegistry.isAnArchive(streamMimeType)) {
                // decompress archive
                if (assetName != null) {
                    sb.append("/");
                    sb.append(assetName);
                } else if (assetId != null) {
                    sb.append("/");
                    sb.append("__asset_id__");
                    sb.append(assetId);
                }
                extractAndTransfer(scp, in, streamMimeType, sb.toString());
            } else {
                // single file
                if (assetName != null) {
                    sb.append("/");
                    sb.append(assetName);
                } else if (assetId != null) {
                    sb.append("/");
                    sb.append("__asset_id__");
                    sb.append(assetId);
                    if (ext != null) {
                        sb.append(".");
                        sb.append(ext);
                    }
                } else {
                    // meta==null
                    if (path == null) {
                        sb.append(System.currentTimeMillis());
                    }
                }
                String dstPath = sb.toString();
                scp.put(in, length, dstPath);
            }
        } finally {
            if (multipleTransferContext == null) {
                if (scp != null) {
                    try {
                        scp.close();
                    } finally {
                        scp.connection().close();
                    }
                }
            }
        }
    }

    private static String generatePath(ServiceExecutor executor, String assetId, String layoutPattern)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        dm.add("expr", layoutPattern);
        return executor.execute("asset.path.generate", dm.root()).value("path");
    }

    private static void extractAndTransfer(ScpPutClient scp, LongInputStream in, String mimeType, String base)
            throws Throwable {

        ArchiveInput ai = ArchiveRegistry.createInput(in, new NamedMimeType(mimeType));
        try {
            ArchiveInput.Entry entry = null;
            while ((entry = ai.next()) != null) {
                try {
                    String dstPath = PathUtils.join(base, entry.name());
                    if (entry.isDirectory()) {
                        scp.mkdirs(dstPath);
                    } else {
                        long size = entry.size();
                        if (size >= 0) {
                            scp.put(entry.stream(), size, dstPath);
                        } else {
                            File tf = PluginTask.createTemporaryFile();
                            try {
                                StreamCopy.copy(entry.stream(), tf);
                                InputStream ti = new BufferedInputStream(new FileInputStream(tf));
                                try {
                                    scp.put(ti, tf.length(), dstPath);
                                } finally {
                                    ti.close();
                                }
                            } finally {
                                PluginTask.deleteTemporaryFile(tf);
                            }
                        }
                    }
                } finally {
                    ai.closeEntry();
                }
            }
        } finally {
            ai.close();
        }
    }

    @Override
    public String description() throws Throwable {
        return "scp sink";
    }

    @Override
    public void endMultiple(Object multipleTransferContext) throws Throwable {
        if (multipleTransferContext != null) {
            ScpPutClient scp = (ScpPutClient) multipleTransferContext;
            try {
                scp.close();
            } finally {
                scp.connection().close();
            }
        }
    }

    @Override
    public Map<String, ParameterDefinition> parameterDefinitions() throws Throwable {
        return ParamDefn.definitions();
    }

    @Override
    public void shutdown() throws Throwable {

    }

    @Override
    public String type() throws Throwable {
        return SINK_TYPE;
    }

}
