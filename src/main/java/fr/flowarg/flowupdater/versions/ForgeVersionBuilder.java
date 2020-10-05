package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.CurseModInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.util.ArrayList;
import java.util.List;

public class ForgeVersionBuilder implements IBuilder<AbstractForgeVersion>
{
    private final ForgeVersionType type;

    public ForgeVersionBuilder(ForgeVersionType type)
    {
        this.type = type;
    }

    private final BuilderArgument<String> forgeVersionArgument = new BuilderArgument<String>("ForgeVersion").required();
    private final BuilderArgument<VanillaVersion> vanillaVersionArgument = new BuilderArgument<VanillaVersion>("VanillaVersion").required();
    private final BuilderArgument<ILogger> loggerArgument = new BuilderArgument<>("Logger", FlowUpdater.DEFAULT_LOGGER).optional();
    private final BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<>("ProgressCallback", FlowUpdater.NULL_CALLBACK).optional();
    private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", new ArrayList<>()).optional();
    private final BuilderArgument<ArrayList<CurseModInfos>> curseModsArgument = new BuilderArgument<ArrayList<CurseModInfos>>("CurseMods", new ArrayList<>()).optional();
    private final BuilderArgument<Boolean> nogGuiArgument = new BuilderArgument<>("NoGui", true).optional();
    private final BuilderArgument<Boolean> useFileDeleterArgument = new BuilderArgument<>("UseFileDeleter", false).optional();

    public ForgeVersionBuilder withForgeVersion(String forgeVersion)
    {
        this.forgeVersionArgument.set(forgeVersion);
        return this;
    }

    public ForgeVersionBuilder withVanillaVersion(VanillaVersion vanillaVersion)
    {
        this.vanillaVersionArgument.set(vanillaVersion);
        return this;
    }

    public ForgeVersionBuilder withLogger(ILogger logger)
    {
        this.loggerArgument.set(logger);
        return this;
    }

    public ForgeVersionBuilder withProgressCallback(IProgressCallback progressCallback)
    {
        this.progressCallbackArgument.set(progressCallback);
        return this;
    }

    public ForgeVersionBuilder withMods(List<Mod> mods)
    {
        this.modsArgument.set(mods);
        return this;
    }

    public ForgeVersionBuilder withCurseMods(ArrayList<CurseModInfos> curseMods)
    {
        this.curseModsArgument.set(curseMods);
        return this;
    }

    public ForgeVersionBuilder withNoGui(boolean noGui)
    {
        this.nogGuiArgument.set(noGui);
        return this;
    }

    public ForgeVersionBuilder withUseFileDeleter(boolean useFileDeleter)
    {
        this.useFileDeleterArgument.set(useFileDeleter);
        return this;
    }

    @Override
    public AbstractForgeVersion build() throws BuilderException
    {
        if(this.progressCallbackArgument.get() == FlowUpdater.NULL_CALLBACK)
            this.loggerArgument.get().warn("You are using default callback for forge installation. If you're using a custom callback for vanilla files, it will not updated when forge and mods will be installed.");
        switch (this.type)
        {
            case NEW:
                return new NewForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.vanillaVersionArgument.get(),
                        this.loggerArgument.get(),
                        this.progressCallbackArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.nogGuiArgument.get(),
                        this.useFileDeleterArgument.get()
                );
            case OLD:
                return new OldForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.vanillaVersionArgument.get(),
                        this.loggerArgument.get(),
                        this.progressCallbackArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.useFileDeleterArgument.get()
                );
            default:
                return null;
        }
    }

    public enum ForgeVersionType
    {
        /** 1.12.2-14.23.5.2851 -> 1.16.3 */
        NEW,
        /** 1.7 -> 1.12.2 */
        OLD
    }
}