package com.mrfuzzihead.additionalpipes;

import java.util.Collection;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import com.mrfuzzihead.additionalpipes.pipes.PipeTeleport;
import com.mrfuzzihead.additionalpipes.pipes.TeleportManager;

public class CommandAdditionalPipes extends CommandBase {

    @Override
    public String getCommandName() {
        return AdditionalPipes.MODID.toLowerCase();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2 || !"teleport".equals(args[0])) {
            return;
        }

        // let's be a bit lenient with plurals
        Collection<? extends PipeTeleport<?>> pipes;
        String type = args[1].toLowerCase();

        if (type.equals("items") || type.equals("item")) {
            pipes = TeleportManager.get()
                .getAllItemPipesInNetwork();
        } else if (type.equals("fluids") || type.equals("fluid")) {
            pipes = TeleportManager.get()
                .getAllFluidPipesInNetwork();
        } else if (type.equals("power")) {
            pipes = TeleportManager.get()
                .getAllPowerPipesInNetwork();
        } else if (type.equals("logistics") || type.equals("logistic")) {
            pipes = TeleportManager.get()
                .getAllLogisticsPipesInNetwork();
        } else {
            return;
        }

        sender.addChatMessage(new ChatComponentText("Teleport pipes: "));
        for (PipeTeleport<?> pipe : pipes) {
            StringBuilder sb = new StringBuilder();
            sb.append('[')
                .append(pipe.getPosition().x)
                .append(", ")
                .append(pipe.getPosition().y)
                .append(", ")
                .append(pipe.getPosition().z)
                .append("] dim ")
                .append(
                    pipe.getContainer()
                        .getWorldObj().provider.dimensionId)
                .append(' ')
                .append(pipe.ownerName);
            sender.addChatMessage(new ChatComponentText(sb.toString()));
        }
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return StatCollector.translateToLocal("command.ap.usage");
    }

}
