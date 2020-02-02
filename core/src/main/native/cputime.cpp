//
// Created by hujian on 2019/11/1.
//

#include <ctime>
#include <sys/times.h>

#include "cputime.h"

// just test !
static   int    clock_tics_per_sec = 100;

// Return the real, user, and system times in seconds from an
// arbitrary fixed point in the past.
bool getTimesSecs(double* process_real_time,
                  double* process_user_time,
                  double* process_system_time) {
    struct tms ticks;
    clock_t real_ticks = times(&ticks);

    if (real_ticks == (clock_t) (-1)) {
        return false;
    } else {
        double ticks_per_second = (double) clock_tics_per_sec;
        *process_user_time = ((double) ticks.tms_utime) / ticks_per_second;
        *process_system_time = ((double) ticks.tms_stime) / ticks_per_second;
        *process_real_time = ((double) real_ticks) / ticks_per_second;

        return true;
    }
}