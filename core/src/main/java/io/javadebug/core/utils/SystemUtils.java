//
//  ========================================================================
//  Copyright (c) 2018-2019 HuJian/Pandening soft collection.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the #{license} Public License #{version}
//  EG:
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  You should bear the consequences of using the software (named 'java-debug-tool')
//  and any modify must be create an new pull request and attach an text to describe
//  the change detail.
//  ========================================================================
//


package io.javadebug.core.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class SystemUtils {

    // hold the mx
    private static final RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();

    /**
     * Returns the name representing the running Java virtual machine.
     * The returned name string can be any arbitrary string and
     * a Java virtual machine implementation can choose
     * to embed platform-specific useful information in the
     * returned name string.  Each running virtual machine could have
     * a different name.
     *
     * @return the name representing the running Java virtual machine.
     */
    public static String getName() {
        return RUNTIME_MX_BEAN.getName();
    }

    /**
     * Returns the Java virtual machine implementation name.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.name")}.
     *
     * @return the Java virtual machine implementation name.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getVmName() {
        return RUNTIME_MX_BEAN.getVmName();
    }

    /**
     * Returns the Java virtual machine implementation vendor.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.vendor")}.
     *
     * @return the Java virtual machine implementation vendor.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getVmVendor() {
        return RUNTIME_MX_BEAN.getVmVendor();
    }

    /**
     * Returns the Java virtual machine implementation version.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.version")}.
     *
     * @return the Java virtual machine implementation version.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getVmVersion() {
        return RUNTIME_MX_BEAN.getVmVersion();
    }

    /**
     * Returns the Java virtual machine specification name.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.specification.name")}.
     *
     * @return the Java virtual machine specification name.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getSpecName() {
        return RUNTIME_MX_BEAN.getSpecName();
    }

    /**
     * Returns the Java virtual machine specification vendor.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.specification.vendor")}.
     *
     * @return the Java virtual machine specification vendor.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getSpecVendor() {
        return RUNTIME_MX_BEAN.getSpecVendor();
    }

    /**
     * Returns the Java virtual machine specification version.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.specification.version")}.
     *
     * @return the Java virtual machine specification version.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getSpecVersion() {
        return RUNTIME_MX_BEAN.getSpecVersion();
    }


    /**
     * Returns the version of the specification for the management interface
     * implemented by the running Java virtual machine.
     *
     * @return the version of the specification for the management interface
     * implemented by the running Java virtual machine.
     */
    public static String getManagementSpecVersion() {
        return RUNTIME_MX_BEAN.getManagementSpecVersion();
    }

    /**
     * Returns the Java class path that is used by the system class loader
     * to search for class files.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.class.path")}.
     *
     * <p> Multiple paths in the Java class path are separated by the
     * path separator character of the platform of the Java virtual machine
     * being monitored.
     *
     * @return the Java class path.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getClassPath() {
        return RUNTIME_MX_BEAN.getClassPath();
    }

    /**
     * Returns the Java library path.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.library.path")}.
     *
     * <p> Multiple paths in the Java library path are separated by the
     * path separator character of the platform of the Java virtual machine
     * being monitored.
     *
     * @return the Java library path.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public static String getLibraryPath() {
        return RUNTIME_MX_BEAN.getLibraryPath();
    }

    /**
     * Tests if the Java virtual machine supports the boot class path
     * mechanism used by the bootstrap class loader to search for class
     * files.
     *
     * @return <tt>true</tt> if the Java virtual machine supports the
     * class path mechanism; <tt>false</tt> otherwise.
     */
    public static boolean isBootClassPathSupported() {
        return RUNTIME_MX_BEAN.isBootClassPathSupported();
    }

    /**
     * Returns the boot class path that is used by the bootstrap class loader
     * to search for class files.
     *
     * <p> Multiple paths in the boot class path are separated by the
     * path separator character of the platform on which the Java
     * virtual machine is running.
     *
     * <p>A Java virtual machine implementation may not support
     * the boot class path mechanism for the bootstrap class loader
     * to search for class files.
     * The {@link #isBootClassPathSupported} method can be used
     * to determine if the Java virtual machine supports this method.
     *
     * @return the boot class path.
     *
     * @throws java.lang.UnsupportedOperationException
     *     if the Java virtual machine does not support this operation.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and the caller does not have
     *     ManagementPermission("monitor").
     */
    public static String getBootClassPath() {
        return RUNTIME_MX_BEAN.getBootClassPath();
    }

    /**
     * Returns the input arguments passed to the Java virtual machine
     * which does not include the arguments to the <tt>main</tt> method.
     * This method returns an empty list if there is no input argument
     * to the Java virtual machine.
     * <p>
     * Some Java virtual machine implementations may take input arguments
     * from multiple different sources: for examples, arguments passed from
     * the application that launches the Java virtual machine such as
     * the 'java' command, environment variables, configuration files, etc.
     * <p>
     * Typically, not all command-line options to the 'java' command
     * are passed to the Java virtual machine.
     * Thus, the returned input arguments may not
     * include all command-line options.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code List<String>} is <tt>String[]</tt>.
     *
     * @return a list of <tt>String</tt> objects; each element
     * is an argument passed to the Java virtual machine.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and the caller does not have
     *     ManagementPermission("monitor").
     */
    public static java.util.List<String> getInputArguments() {
        return RUNTIME_MX_BEAN.getInputArguments();
    }

    /**
     * Returns the uptime of the Java virtual machine in milliseconds.
     *
     * @return uptime of the Java virtual machine in milliseconds.
     */
    public static long getUptime() {
        return RUNTIME_MX_BEAN.getUptime();
    }

    /**
     * Returns the start time of the Java virtual machine in milliseconds.
     * This method returns the approximate time when the Java virtual
     * machine started.
     *
     * @return start time of the Java virtual machine in milliseconds.
     *
     */
    public static long getStartTime() {
        return RUNTIME_MX_BEAN.getStartTime();
    }

    /**
     * Returns a map of names and values of all system properties.
     * This method calls {@link System#getProperties} to get all
     * system properties.  Properties whose name or value is not
     * a <tt>String</tt> are omitted.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code Map<String,String>} is
     * {@link javax.management.openmbean.TabularData TabularData}
     * with two items in each row as follows:
     * <blockquote>
     * <table border summary="Name and Type for each item">
     * <tr>
     *   <th>Item Name</th>
     *   <th>Item Type</th>
     *   </tr>
     * <tr>
     *   <td><tt>key</tt></td>
     *   <td><tt>String</tt></td>
     *   </tr>
     * <tr>
     *   <td><tt>value</tt></td>
     *   <td><tt>String</tt></td>
     *   </tr>
     * </table>
     * </blockquote>
     *
     * @return a map of names and values of all system properties.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to the system properties.
     */
    public static java.util.Map<String, String> getSystemProperties() {
        return RUNTIME_MX_BEAN.getSystemProperties();
    }

}
