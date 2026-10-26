package com.mrfuzzihead.additionalpipes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mrfuzzihead.additionalpipes.utils.Log;

/**
 * Optional Logistics Pipes integration, isolated so LP classes are only touched when LP is installed.
 */
final class LPCompat {

    private LPCompat() {}

    static void register(boolean installed) {
        if (!installed) {
            Log.info("Logistics Pipes not detected");
            return;
        }

        Log.info("Commencing morass of reflection to try and integrate with Logistics Pipes");
        boolean success = false;
        try {
            Class<?> SimpleServiceLocator = Class.forName("logisticspipes.proxy.SimpleServiceLocator");
            Class<?> SpecialPipeConnection = Class
                .forName("logisticspipes.proxy.specialconnection.SpecialPipeConnection");
            Class<?> ISpecialPipedConnection = Class
                .forName("logisticspipes.interfaces.routing.ISpecialPipedConnection");
            Class<?> APSpecialPipedConnection = Class
                .forName("com.mrfuzzihead.additionalpipes.pipes.APSpecialPipedConnection");

            Field specialpipeconnectionField = SimpleServiceLocator.getDeclaredField("specialpipeconnection");
            Object specialpipeconnection = specialpipeconnectionField.get(null);
            Method registerHandler = SpecialPipeConnection
                .getDeclaredMethod("registerHandler", ISpecialPipedConnection);
            Object apSpecialPC = APSpecialPipedConnection.newInstance();
            registerHandler.invoke(specialpipeconnection, apSpecialPC);

            success = true;
        } catch (Exception e) {
            Log.error("Logistics Pipes integration failed", e);
        }

        Log.info("Integration " + (success ? "succeeded" : "failed"));
    }
}
