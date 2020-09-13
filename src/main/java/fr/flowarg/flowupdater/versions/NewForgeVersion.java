package fr.flowarg.flowupdater.versions;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getSHA1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.IOUtils;

/**
 * Represent a new Forge version (1.12.2-14.23.5.2851 -> 1.16.1)
 * @author FlowArg
 */
public class NewForgeVersion implements IForgeVersion
{
	private final String[] compatibleVersions = {"1.16", "1.15", "1.14", "1.12.2-14.23.5.285"};
	private final boolean noGui;
    private final ILogger logger;
    private String forgeVersion;
    private URL installerUrl;
    private IProgressCallback callback;
    private List<Mod> mods;
    private boolean useFileDeleter = false;
    private DownloadInfos downloadInfos;

    public NewForgeVersion(String forgeVersion, VanillaVersion vanilla, ILogger logger, IProgressCallback callback, List<Mod> mods, boolean noGui)
    {
        this.logger = logger;
        this.noGui = noGui;
        try
        {
            if (!forgeVersion.contains("-"))
                this.forgeVersion = vanilla.getName() + '-' + forgeVersion;
            else this.forgeVersion = forgeVersion.trim();
            this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", this.forgeVersion, this.forgeVersion));
            this.callback = callback;
            this.mods = mods;
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void install(final File dirToInstall)
    {
    	this.callback.step(Step.FORGE);
    	this.logger.info("Installing new forge version : " + this.forgeVersion + "...");
    	if (this.isCompatible())
        {
            try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
            {
                this.logger.info("Downloading new forge installer...");
                final File tempDir = new File(dirToInstall, ".flowupdater");
                final File tempInstallerDir = new File(tempDir, "installer/");
                final File install = new File(tempDir, "forge-installer.jar");
                final File patches = new File(tempDir, "patches.jar");
                final File patchedInstaller = new File(tempDir, "forge-installer-patched.jar");
                FileUtils.deleteDirectory(tempInstallerDir);
                install.delete();
                patchedInstaller.delete();
                patches.delete();
                tempDir.mkdirs();
                tempInstallerDir.mkdirs();

                Files.copy(stream, install.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.logger.info("Downloading patches...");
                Files.copy(new URL("https://flowarg.github.io/minecraft/launcher/patches.jar").openStream(), patches.toPath(), StandardCopyOption.REPLACE_EXISTING);

                this.logger.info("Applying patches...");
                FileUtils.unzipJarWithLZMACompat(tempInstallerDir, install);
                this.cleaningInstaller(tempInstallerDir);
                FileUtils.unzipJarWithLZMACompat(tempInstallerDir, patches);
                this.logger.info("Repack installer...");
                this.packPatchedInstaller(tempDir, tempInstallerDir);
                patches.delete();
                this.logger.info("Launching forge installer...");
                
                final ArrayList<String> command = new ArrayList<>();
                command.add("java");
                command.add("-Xmx256M");
                command.add("-jar");
                command.add(patchedInstaller.getAbsolutePath());
                command.add("--installClient");
                command.add(dirToInstall.getAbsolutePath());
                if(this.noGui)
                	command.add("--nogui");
                final ProcessBuilder processBuilder = new ProcessBuilder(command);
                
                processBuilder.redirectOutput(Redirect.INHERIT);
                final Process process = processBuilder.start();
                process.waitFor();
                
                this.logger.info("Successfully installed Forge !");
                FileUtils.deleteDirectory(tempDir);
            }
            catch (IOException | InterruptedException e)
            {
                this.logger.printStackTrace(e);
            }
        }
    }
    
    public boolean isCompatible()
    {
    	for(String str : this.compatibleVersions)
    	{
    		if(this.forgeVersion.startsWith(str))
    			return true;
    	}
    	return false;
    }

    private void cleaningInstaller(File tempInstallerDir)
    {
        FileUtils.deleteDirectory(new File(tempInstallerDir, "net"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "com"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "joptisimple"));
        new File(tempInstallerDir, "META-INF/MANIFEST.MF").delete();
        new File(tempInstallerDir, "lekeystore.jks").delete();
        new File(tempInstallerDir, "big_logo.png").delete();
        new File(tempInstallerDir, "META-INF/FORGE.DSA").delete();
        new File(tempInstallerDir, "META-INF/FORGE.SF").delete();
    }
    
	@Override
	public void installMods(File modsDir) throws IOException
	{
		this.callback.step(Step.MODS);
		this.downloadInfos.getMods().forEach(mod -> {
			try
			{
				IOUtils.download(this.logger, new URL(mod.getDownloadURL()), new File(modsDir, mod.getName()));
			}
			catch (MalformedURLException e)
			{
				this.logger.printStackTrace(e);
			}
			this.downloadInfos.incrementDownloaded();
			this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
		});
		
		if(this.useFileDeleter)
		{
			final List<File> badFiles = new ArrayList<>();
			final List<File> verifiedFiles = new ArrayList<>();
			for(File fileInDir : modsDir.listFiles())
			{
				if(!fileInDir.isDirectory())
				{
					for(Mod mod : this.mods)
					{
						final File file = new File(modsDir, mod.getName().endsWith(".jar") ? mod.getName() : mod.getName() + ".jar");
						if(file.getName().equalsIgnoreCase(fileInDir.getName()))
						{
							if(getSHA1(fileInDir).equals(mod.getSha1()) && getFileSizeBytes(fileInDir) == mod.getSize())
							{
								if(badFiles.contains(fileInDir))
									badFiles.remove(fileInDir);
								verifiedFiles.add(fileInDir);
							}
							else badFiles.add(fileInDir);
						}
						else
						{
							if(!verifiedFiles.contains(fileInDir))
								badFiles.add(fileInDir);
						}
					}
				}
			}
			
			badFiles.forEach(File::delete);
			badFiles.clear();
		}
	}

    public ILogger getLogger()
    {
        return this.logger;
    }

    public String getForgeVersion()
    {
        return this.forgeVersion;
    }

    public URL getInstallerUrl()
    {
        return this.installerUrl;
    }
    
    @Override
    public List<Mod> getMods()
    {
		return this.mods;
	}
    
    public void setMods(List<Mod> mods)
    {
		this.mods = mods;
	}
    
    public boolean isNoGui()
    {
		return this.noGui;
	}

	@Override
	public boolean isModFileDeleterEnabled()
	{
		return this.useFileDeleter;
	}

	@Override
	public IForgeVersion enableModFileDeleter()
	{
		this.useFileDeleter = true;
		return this;
	}
	
	@Override
	public IForgeVersion disableModFileDeleter()
	{
		this.useFileDeleter = false;
		return this;
	}

	@Override
	public boolean isForgeAlreadyInstalled(File installDir)
	{
		return new File(installDir, "libraries/net/minecraftforge/forge/" + this.forgeVersion + "/" + "forge-" + this.forgeVersion + ".jar").exists();
	}

	@Override
	public void appendDownloadInfos(DownloadInfos infos)
	{
		this.downloadInfos = infos;
	}
}
