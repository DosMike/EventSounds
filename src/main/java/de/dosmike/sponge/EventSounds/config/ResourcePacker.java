package de.dosmike.sponge.EventSounds.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import de.dosmike.sponge.EventSounds.EventSounds;
import de.dosmike.sponge.EventSounds.sounds.EventSoundRegistry;
import it.sauronsoftware.ftp4j.*;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.resourcepack.ResourcePacks;
import org.spongepowered.api.text.Text;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ResourcePacker {

    private static final Path soundsJson = Paths.get("assets/minecraft/sounds.json");
    private static final Path soundsDir = Paths.get("assets/minecraft/sounds");
    private static final Path soundsCustom = soundsDir.resolve("custom/es/server");
    /** returns the path inside the resource-pack where sounds are located */
    public static Path getPackDir() {
        return soundsCustom;
    }
    private String ftpU, ftpP, sha1=null;

    private File zipSrc, zipDst = new File(".", "eventsounds_pack.zip");;
    private URL ftpTarget;
    private JsonObject jsonSounds;
    public ResourcePacker(String templateArchive, String ftpUploadTarget, String ftpUser, String ftpPass) throws MalformedURLException {
        if (templateArchive != null)
            zipSrc = new File(".", templateArchive);
        else zipSrc = null;
        if (ftpUploadTarget == null || ftpUploadTarget.isEmpty()) {
            ftpTarget = null;
        } else {
            ftpTarget = new URL(ftpUploadTarget);
            if ((ftpPass == null || ftpPass.isEmpty()) && (ftpUser != null && !ftpUser.isEmpty()))
                throw new RuntimeException("A ftp password is required if a ftp user was set");
            else if (ftpUser != null && !ftpUser.isEmpty()) {
                ftpU = ftpUser;
                ftpP = ftpPass;
            }
        }
        jsonSounds = new JsonObject();
    }

    public void repack() throws IOException {
        Set<String> zipped = new HashSet<>();
        EventSounds.l("Generating %s...",zipDst.getName());
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipDst));
        zout.setMethod(ZipOutputStream.DEFLATED);
        zout.setLevel(9);

        if (zipSrc != null && zipSrc.exists()) {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipSrc));
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                Path path = Paths.get(entry.getName());
                if (path.startsWith(soundsDir) || path.equals(soundsJson)) continue;

                EventSounds.l("Repacking %s", entry.getName());
                zout.putNextEntry(new ZipEntry(entry.getName()));

                byte[] buffer = new byte[512]; int r;
                while ((r=zin.read(buffer,0,buffer.length))>=0) {
                    zout.write(buffer,0,r);
                }
                zin.closeEntry();
                zout.closeEntry();
            }
        } else {
            JsonObject packmcmeta = new JsonObject();

            JsonObject jpack = new JsonObject();
            jpack.addProperty("description", "Automatically generated Resource-Pack\nBy: EventSounds Sponge-Plugin");
            jpack.addProperty("pack_format", 3);
            packmcmeta.add("pack", jpack);

            ZipEntry entry = new ZipEntry("pack.mcmeta");
            zout.putNextEntry(entry);
            {
                EventSoundRegistry.getSoundDefinitions().forEach(def -> jsonSounds.add(def.getRegistryName(), def.toJson()));
                StringWriter sw = new StringWriter(512);
                JsonWriter writer = new JsonWriter(sw);
                Gson g = new Gson();
                g.toJson(jsonSounds, writer);
                zout.write(sw.toString().getBytes());
            }
            zout.closeEntry();
        }

        //replace('\', '/'), because minecraft does not seem to use Paths, but split('/')
        ZipEntry entry = new ZipEntry(soundsJson.toString().replace('\\','/'));
        zout.putNextEntry(entry);
        {
            EventSoundRegistry.getSoundDefinitions().forEach(def -> jsonSounds.add(def.getRegistryName(), def.toJson()));
            StringWriter sw = new StringWriter(512);
            JsonWriter writer = new JsonWriter(sw);
            Gson g = new Gson();
            g.toJson(jsonSounds, writer);
            zout.write(sw.toString().getBytes());
        }
        zout.closeEntry();

        EventSoundRegistry.getSoundDefinitions().forEach(def->{
            if (!def.isExternal()) {
                for (String file : def.getFiles()) {
                    if (zipped.contains(file)) continue;
                    zipped.add(file);
                    File toZip = new File("./assets/eventsounds/", file);

                    //replace('\', '/'), because minecraft does not seem to use Paths, but split('/')
                    String zipPath = soundsCustom.resolve(file).toString().replace('\\', '/');
                    EventSounds.l("Zipping %s to %s", toZip.getAbsolutePath(), zipPath);
                    ZipEntry e = new ZipEntry(zipPath);
                    InputStream fin = null;
                    try {
                        fin = new FileInputStream(toZip);
                        zout.putNextEntry(e);
                        byte[] buffer = new byte[1024];
                        int r;
                        while ((r = fin.read(buffer, 0, buffer.length)) >= 0) {
                            zout.write(buffer, 0, r);
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } finally {
                        try {
                            fin.close();
                        } catch (IOException ignore) {
                            /**/
                        }
                        try {
                            zout.closeEntry();
                        } catch (Exception ignore) {
                            /**/
                        }
                    }
                }
            }
        });

        //adding sounds from other plugins
        SoundCollector.copyToResourcePack(zout);

        zout.finish();
        zout.flush();
        zout.close();

    }

    /** updates the sha1 value in server.properties and
     * uploads the resource-pack via FTP */
    public void upload() throws IOException, NoSuchAlgorithmException, URISyntaxException {

        if (!"ftp".equalsIgnoreCase(ftpTarget.getProtocol())) {
            throw new IOException("Protocol not supported: "+ftpTarget.getProtocol());
        }

        byte[] buffer = new byte[512];
        int r;

        //calculate sha1
        sha1=null;
        {
            FileInputStream fis = new FileInputStream(zipDst);
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            while ((r = fis.read(buffer)) >= 0)
                digest.update(buffer, 0, r);
            fis.close();
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02X", b));
            sha1 = sb.toString();
        }

        EventSounds.l("Patching server.properties value resource-pack-sha1 to %s", sha1);
        {
            File propertiesFile = new File("server.properties");
            StringBuilder props = new StringBuilder();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(propertiesFile)));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("resource-pack-sha1=")) {
                        props.append("resource-pack-sha1=" + sha1);
                    } else {
                        props.append(line);
                    }
                    props.append('\n');
                }
            } catch (IOException error) {
                error.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (Exception ignore) {/**/}
            }
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propertiesFile)));
                bw.write(props.toString());
                bw.flush();
            } catch (IOException error) {
                error.printStackTrace();
            } finally {
                try {
                    bw.close();
                } catch (Exception ignore) {/**/}
            }
        }

        EventSounds.l("Establishing FTP connection");

        FTPClient client = new FTPClient();
        InputStream zipIn = null;
        try {
            zipIn = new FileInputStream(zipDst);

            int port = ftpTarget.getPort();
            if (port < 0) //no port specified
                client.connect(ftpTarget.getHost());
            else
                client.connect(ftpTarget.getHost(), port);
            if (ftpU != null) {
                try {
                    client.login(ftpU, ftpP);
                    client.setAutoNoopTimeout(15000);
                } catch (FTPException|FTPIllegalReplyException exception) {
                    throw new RuntimeException("Login failed"); // more readable error for user
                }
            }

            Path path = Paths.get(ftpTarget.getPath());
            if (path.getFileName() == null || !path.getFileName().toString().toLowerCase().endsWith(".zip"))
                throw new IOException("Please append a zip path to the ftp url");
            EventSounds.l("Remote file: %s",path.toString());

            for (int i = 0; i < path.getNameCount() - 1; i++) {
                String elem = path.getName(i).toString();
                forceEnterDirectory(client, elem);
            }
            String name = path.getName(path.getNameCount()-1).toString();
            String[] files = client.listNames();
            if (files == null)
                throw new IOException("Directory could not be read");
            if (ArrayUtils.contains(files, name)) {
                EventSounds.l("FTP> rm %s", name);
                try {
                    client.deleteFile(name);
                } catch (Exception e) {
                    throw new IOException("Could not delete old resource-pack");
                }
            }
            EventSounds.l("FTP> put %s", name);
            client.setType(FTPClient.TYPE_BINARY);
            try {
                client.upload(name, zipIn, 0, 0, null);
            } catch (FTPDataTransferException|FTPAbortedException e) {
                throw new RuntimeException("Upload failed or was aborted");
            }

            client.logout();
        } catch (Exception e) {
            throw new RuntimeException("FTP connection Failed", e);
        } finally {
            try { client.disconnect(true); } catch (Exception e) {/**/}
            try { zipIn.close(); } catch (Exception e) {/**/}
        }
    }

    public boolean validateResourcePack() {
        if (sha1 == null) return false;
        try {
            //use to extract uri
            ResourcePack pack = Sponge.getServer().getDefaultResourcePack().orElse(null);
            if (pack==null)return false;
            //get updated hash
            pack = ResourcePacks.fromUri(pack.getUri());
            EventSounds.l("Hash A/B: \n%s\n%s", pack.getHash().orElse("NULL"), sha1);
            return sha1.equalsIgnoreCase(pack.getHash().orElse(null));
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private void forceEnterDirectory(FTPClient client, String directory) throws FTPException, FTPIllegalReplyException, IOException {
        try {
            EventSounds.l("FTP> cd %s", directory);
            client.changeDirectory(directory);
        } catch (FTPException e) {
            EventSounds.l("FTP> mkdir %s", directory);
            client.createDirectory(directory);
            EventSounds.l("FTP> cd %s", directory);
            client.changeDirectory(directory);
        }
    }

    public void sendDefaultPackUpdated(Player player) {
        ResourcePack pack = Sponge.getServer().getDefaultResourcePack().orElse(null);
        if (pack==null) {
            player.sendMessage(Text.of("Resource pack download was not defined in server.properties"));
            return;
        }
        ResourcePack newPack = ResourcePacks.fromUriUnchecked(pack.getUri());
        player.sendResourcePack(newPack);
    }
}
