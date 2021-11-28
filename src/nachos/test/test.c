#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main() {
    char buffer[5000];
    int count=5000;
    int fd = open("testin3g.txt");

    if(fd == -1) {
        printf("Unable to open file");
        exit(0);
    }
    count = read(fd, buffer, count);

    write(fdStandardOutput, buffer, count);
}
