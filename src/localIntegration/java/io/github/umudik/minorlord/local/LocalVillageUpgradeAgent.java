package io.github.umudik.minorlord.local;
import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
public final class LocalVillageUpgradeAgent {
    public static void agentmain(String ignored,Instrumentation inst)throws Exception {
        Class<?> loader=null,mod=null;
        for(var c:inst.getAllLoadedClasses()){
            if(c.getName().equals("net.fabricmc.loader.impl.FabricLoaderImpl"))loader=c;
            if(c.getName().equals("io.github.umudik.minorlord.MinorLord"))mod=c;
        }
        if(mod==null)throw new IllegalStateException("Minor Lord not loaded");
        var server=loader.getMethod("getGameInstance").invoke(loader.getField("INSTANCE").get(null));
        var jar=ignored!=null&&!ignored.isBlank()?java.nio.file.Path.of(ignored).toUri().toURL():LocalVillageUpgradeAgent.class.getProtectionDomain().getCodeSource().getLocation();
        var checks=new URLClassLoader(new java.net.URL[]{jar},mod.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(name.startsWith("io.github.umudik.minorlord.local.")&&!name.contains("LocalVillageUpgradeAgent")){
                    synchronized(getClassLoadingLock(name)){
                        var loaded=findLoadedClass(name);if(loaded==null)try{loaded=findClass(name);}catch(ClassNotFoundException missing){return super.loadClass(name,resolve);}
                        if(resolve)resolveClass(loaded);return loaded;
                    }
                }return super.loadClass(name,resolve);
            }
        };
        var entry=checks.loadClass("io.github.umudik.minorlord.local.LocalVillageUpgrade");
        var base=mod.getClassLoader().loadClass("net.minecraft.server.MinecraftServer");
        server.getClass().getMethod("execute",Runnable.class).invoke(server,(Runnable)()->{
            try{entry.getMethod("run",base).invoke(null,server);}catch(Throwable failure){failure.printStackTrace();}
        });
    }
}

