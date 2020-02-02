//
// Created by hujian on 2019/11/1.
//

#ifndef CPUTIME_CPUTIME_H
#define CPUTIME_CPUTIME_H

// Return the real, user, and system times in seconds from an
// arbitrary fixed point in the past.
extern "C"
bool getTimesSecs(double* process_real_time, /* walk clock time */
                  double* process_user_time, /*  User CPU time for all threads */
                  double* process_system_time /* System CPU time */
                  );

#endif //CPUTIME_CPUTIME_H
