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


package io.javadebug.core.data;

import sun.management.ManagementFactoryHelper;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.util.List;

/**
 *  the memory information
 *
 *  Auth : pandening
 *  Date : 2020-01-11 23:37
 */
public class JVMMemoryInfo {

    // heap size eden + survivor (from + to) + old
    private long usedHeapMem = -1;
    private long maxHeapMem = -1;

    // young -> eden
    private long usedEdenMem = -1;
    private long maxEdenMem = -1;

    // young -> survivor
    private long usedSurvivorMem = -1;
    private long maxSurvivorMem = -1;

    // old gen
    private long usedOldGenMem = -1;
    private long maxOldGenMem = -1;

    // metaspace
    private long usedMetaspaceMem = -1;
    private long maxMetaspaceMem = -1;

    // non-heap
    private long usedNonHeapMem = -1;
    private long maxNonHeapMem = -1;

    // physical (mem + swap)
    private long usedSwapMem = -1;
    private long maxSwapMem = -1;
    private long usedPhysicalMem = -1;
    private long maxPhysicalMem = -1;

    // direct & mapped
    private long maxDirectMem = -1;
    private long usedDirectMem = -1;
    private long maxMappedMem = -1;
    private long usedMappedMem = -1;

    public JVMMemoryInfo() {

        // init heap mem
        usedHeapMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        maxHeapMem = Runtime.getRuntime().maxMemory();

        // young gen [Eden]
        MemoryPoolMXBean youngGen = getSpecialGenMemoryPool("Eden");
        if (youngGen != null) {
            usedEdenMem = youngGen.getUsage().getUsed();
            maxEdenMem = youngGen.getUsage().getMax();
        }

        // young gen [survivor]
        MemoryPoolMXBean survivorGen = getSpecialGenMemoryPool("Survivor");
        if (survivorGen != null) {
            usedSurvivorMem = survivorGen.getUsage().getUsed();
            maxSurvivorMem = survivorGen.getUsage().getMax();
        }

        // metaspace
        MemoryPoolMXBean metaspace = getSpecialGenMemoryPool("Metaspace");
        if (metaspace != null) {
            usedMetaspaceMem = metaspace.getUsage().getUsed();
            maxMetaspaceMem = metaspace.getUsage().getMax();
        }

        // old gen
        MemoryPoolMXBean oldGen = getSpecialGenMemoryPool("Old");
        if (oldGen != null) {
            usedOldGenMem = oldGen.getUsage().getUsed();
            maxOldGenMem = oldGen.getUsage().getMax();
        }

        // non-heap
        usedNonHeapMem = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
        maxNonHeapMem = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();

        // direct & mapped
        List<BufferPoolMXBean> bufferPoolMXBeanList = ManagementFactoryHelper.getBufferPoolMXBeans();
        BufferPoolMXBean directPool = getDirectOrMappedMem(bufferPoolMXBeanList, "direct");
        BufferPoolMXBean mappedPool = getDirectOrMappedMem(bufferPoolMXBeanList, "mapped");

        if (directPool != null) {
            maxDirectMem = directPool.getTotalCapacity();
            usedDirectMem = directPool.getMemoryUsed();
        }

        if (mappedPool != null) {
            maxMappedMem = mappedPool.getTotalCapacity();
            usedMappedMem = mappedPool.getMemoryUsed();
        }

        // physic
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystem instanceof com.sun.management.OperatingSystemMXBean) {
            usedPhysicalMem = ((com.sun.management.OperatingSystemMXBean) operatingSystem).getTotalPhysicalMemorySize()
                    - ((com.sun.management.OperatingSystemMXBean) operatingSystem).getFreePhysicalMemorySize();
            maxPhysicalMem = ((com.sun.management.OperatingSystemMXBean) operatingSystem).getTotalPhysicalMemorySize();

            usedSwapMem = ((com.sun.management.OperatingSystemMXBean) operatingSystem).getTotalSwapSpaceSize()
                    - ((com.sun.management.OperatingSystemMXBean) operatingSystem).getFreeSwapSpaceSize();
            maxSwapMem = ((com.sun.management.OperatingSystemMXBean) operatingSystem).getTotalSwapSpaceSize();
        }

    }

    /**
     *  "direct"
     *  "mapped"
     *
     * @param bufferPoolMXBeanList {@link ManagementFactoryHelper#getBufferPoolMXBeans()}
     * @param key  which pool
     * @return the target pool
     */
    private static BufferPoolMXBean getDirectOrMappedMem(List<BufferPoolMXBean> bufferPoolMXBeanList, String key) {
        for (BufferPoolMXBean bufferPoolMXBean : bufferPoolMXBeanList) {
            if (bufferPoolMXBean.getName().contains(key)) {
                return bufferPoolMXBean;
            }
        }
        return null;
    }

    /**
     *  get special gen memory pool {@link MemoryPoolMXBean}
     *
     * @param gen the target gen like 'Old Gen' / 'Old'
     * @return the target pool, maybe null
     */
    private static MemoryPoolMXBean getSpecialGenMemoryPool(String gen) {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getName().contains(gen)) {
                return memoryPool;
            }
        }
        return null;
    }

    private static final String MEM_SHOW_FORMAT = "%-30.30s " + "%-30.30s " + "%-30.30s";
    private static final String MEM_TITLE = String.format(MEM_SHOW_FORMAT, "Heap", "Non-Heap", "Physical");

    @Override
    public String toString() {

        String ph = String.format(MEM_SHOW_FORMAT, "--------", "--------", "--------");
        StringBuilder rsb = new StringBuilder();

        // header
        rsb.append(ph).append("\n").append(MEM_TITLE).append("\n").append(ph).append("\n");

        // line 1
        String line = String.format(MEM_SHOW_FORMAT,
                "|_Heap      :[" + tramMem(maxHeapMem) + "," + tramMem(usedHeapMem) + "]",
                "|_NonHeap   :[" + tramMem(maxNonHeapMem) + "," + tramMem(usedNonHeapMem) + "]",
                "|_Physical :[" + tramMem(maxPhysicalMem) + "," + tramMem(usedPhysicalMem) + "]"
                );
        rsb.append(line).append("\n");

        // line 2
        line = String.format(MEM_SHOW_FORMAT,
                "|_Eden      :[" + tramMem(maxEdenMem) + "," + tramMem(usedEdenMem) + "]",
                "|_Direct    :[" + tramMem(maxDirectMem) + "," + tramMem(usedDirectMem) + "]",
                "|_Swap     :[" + tramMem(maxSwapMem) + "," + tramMem(usedSwapMem) + "]"
        );
        rsb.append(line).append("\n");

        // line 3
        line = String.format(MEM_SHOW_FORMAT,
                "|_Survivor  :[" + tramMem(maxSurvivorMem) + "," + tramMem(usedSurvivorMem) + "]",
                "|_Mapped    :[" + tramMem(maxMappedMem) + "," + tramMem(usedMappedMem) + "]",
                ""
        );
        rsb.append(line).append("\n");

        // line 4
        line = String.format(MEM_SHOW_FORMAT,
                "|_OldGen    :[" + tramMem(maxOldGenMem) + "," + tramMem(usedOldGenMem) + "]",
                "|_Metaspace :[" + tramMem(maxMetaspaceMem) + "," + tramMem(usedMetaspaceMem) + "]",
                ""
        );
        rsb.append(line).append("\n");

        //result
        return rsb.toString();
    }

    private static String tramMem(long bytes) {
        if (bytes < 0) {
            return bytes + "";
        }
        if (bytes <= 1024) { // b
            return bytes + " b";
        } else if (bytes <= (1024.0 * 1024.0)) {
            return  trimDouble((bytes) / (1024.0 * 1024.0), 1) + "kb";
        } else if (bytes <= (1024.0 * 1024.0 * 1024)) {
            return trimDouble((bytes / (1024.0 * 1024.0)), 1) + "mb";
        } else if (bytes <= (1024.0 * 1024.0 * 1024.0 * 1024)) {
            return trimDouble((bytes / (1024.0 * 1024.0 * 1024.0)), 2) + "gb";
        } else if (bytes <= (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024)) {
            return trimDouble((bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0)), 2) + "tb";
        } else {
            return trimDouble((bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0)), 3) + "pb";
        }
    }

    private static double trimDouble(double d, int s) {
        BigDecimal bigDecimal = new BigDecimal(d);
        bigDecimal = bigDecimal.setScale(s, BigDecimal.ROUND_HALF_UP);
        return bigDecimal.doubleValue();
    }

    public long getUsedHeapMem() {
        return usedHeapMem;
    }

    public long getMaxHeapMem() {
        return maxHeapMem;
    }

    public long getUsedEdenMem() {
        return usedEdenMem;
    }

    public long getMaxEdenMem() {
        return maxEdenMem;
    }

    public long getUsedSurvivorMem() {
        return usedSurvivorMem;
    }

    public long getMaxSurvivorMem() {
        return maxSurvivorMem;
    }

    public long getUsedOldGenMem() {
        return usedOldGenMem;
    }

    public long getMaxOldGenMem() {
        return maxOldGenMem;
    }

    public long getUsedMetaspaceMem() {
        return usedMetaspaceMem;
    }

    public long getMaxMetaspaceMem() {
        return maxMetaspaceMem;
    }

    public long getUsedNonHeapMem() {
        return usedNonHeapMem;
    }

    public long getMaxNonHeapMem() {
        return maxNonHeapMem;
    }

    public long getUsedSwapMem() {
        return usedSwapMem;
    }

    public long getMaxSwapMem() {
        return maxSwapMem;
    }

    public long getUsedPhysicalMem() {
        return usedPhysicalMem;
    }

    public long getMaxPhysicalMem() {
        return maxPhysicalMem;
    }

    public long getMaxDirectMem() {
        return maxDirectMem;
    }

    public long getUsedDirectMem() {
        return usedDirectMem;
    }

    public long getMaxMappedMem() {
        return maxMappedMem;
    }

    public long getUsedMappedMem() {
        return usedMappedMem;
    }
}
