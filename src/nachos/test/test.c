#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#define N 14


int fds[N];
char filenames[N][32];
char buffers[N][256];

// filename including null-terminating character is 256
char longFileName[256] = "frdrIAp8IBVtwzdj0q4ekuunPCztrSVewUmRDReHUEtO2NeG3fvNQylHPyTcLcZ36Cn6NnThJI4L7LK0unOY2Max6aYFezpTuLAHsjW9iUjtYykGBUmiBGozdkkwtN0sOnQRQClQVgnaEGepxEsdI3amxa0c5yriiJB0lZKEztParZE0eQVTmVrNgfBq8vxYkc3l1Ssf3cQDQgQB5d2npKxczlYzHXL0C4wF0N5GmykiKmj6XxUgsnlcbkxmKff";

// this filename is too long because including the null-terminating character, it is 257 characters long
char tooLongFileName[256] = "frrIAp8IBVtwzj0q4ekuunPCztrSVewUmRDReHUEtO2NeG3fvNQylHPyTcLcZ36Cn6NnThJI4L7LK0unOY2Max6aYFezpTuLAHsjW9iUjtYykGBUmiBGozdkkwtN0sOnQRQClQVgnaEGepxEsdI3amxa0c5yriiJB0lZKEztParZE0eZQVTmVrNgfBq8vxYkc3l1Ssf3cQDQgQB5d2npKxczlYzHXL0C4wF0N5GmykiKmj6XxUgsnlcbkxmKxffd";


int testCreatLongName() {
    if(creat(longFileName) == -1) {
        printf("creat test failed at accepting filename size: 256\n");
    }
    else {
        printf("creat test succeeded at accepting filename size: 256\n");
    }

    if(creat(tooLongFileName) == -1) {
        printf("creat test succeeded at rejecting filename size: 257\n");
    }
    else {
        printf("creat test failed at rejecting filename size: 257\n");
    }
}
int testOpenLongName() {
    if(open(longFileName) == -1) {
        printf("open test failed at accepting filename size: 256\n");
    }
    else {
        printf("open test succeeded at accepting filename size: 256\n");
    }

    if(open(tooLongFileName) == -1) {
        printf("open test succeeded at rejecting filename size: 257\n");
    }
    else {
        printf("open test failed at rejecting filename size: 257\n");
    }
}
int testUnlinkLongName() {
    if(unlink(longFileName) == -1) {
        printf("unlink test failed at accepting filename size: 256\n");
    }
    else {
        printf("unlink test succeeded at accepting filename size: 256\n");
    }

    if(unlink(tooLongFileName) == -1) {
        printf("unlink test succeeded at rejecting filename size: 257\n");
    }
    else {
        printf("unlink test failed at rejecting filename size: 257\n");
    }
}

void createTestFileNames() {
    int i;
    for(i = 0; i < N; i++) 
        sprintf(filenames[i], "filename%d", i);
}

int testCreat() {
    printf("\n");
    int i;
    for(i = 0; i < N; i++){
        int fd = creat(filenames[i]);
        if(fd == -1) {
            printf("Could not create file '%s'\n", filenames[i]);
        }
        else {
            fds[i] = fd;
            printf("Successfully created '%s'\t\tfd: %d\n", filenames[i], fds[i]);
        }
    }
    return 0;
}

int testOpen() {
    printf("\n");
    int i;
    for(i = 0; i < N; i++){
        int fd = open(filenames[i]);
        if(fd == -1) {
            printf("Could not open file '%s'\n", filenames[i]);
        }
        else {
            fds[i] = fd;
            printf("Successfully opened '%s'\t\tfd: %d\n", filenames[i], fds[i]);
        }
    }
    return 0;
}

int testClose() {
    printf("\n");
    // test close() with N files
    int i;
    for(i = 0; i < N; i++){
        if(close(fds[i]) == -1) {
            printf("Could not close file descriptor: %d\n", fds[i]);
        }
        else {
            printf("Closed file descriptor: %d\n", fds[i]);
        }
    }
    return 0;
}

int testUnlink() {

    printf("\n");
    int i;
    for(i = 0; i < N; i++){
        int fd = unlink(filenames[i]);
        if(fd == -1) {
            printf("Could not delete file '%s'\n", filenames[i]);
        }
        else {
            printf("Deleted: '%s'\n", filenames[i]);
        }
    }
    return 0;
}

void createRandomBuffers() {
    int i; 
    for(i = 0; i < N; i++) {
        int j;
        for(j = 0; j < 255; j++) {
            buffers[i][j] = ((i+j)%79)+48;
        }
    }
}

// Goes through all file descriptors and tests reading and writing on them
// For each file descriptor the following will happen:
// 1) Write a buffer to file descriptor
// 2) Flush the buffer (close and open)
// 3) Read buffer from file descriptor into tempBuffer
// 4) Compare buffers
int testReadAndWrite() {

    printf("\n");
    int i;
    for(i = 0; i < N; i++) {
        char tempBuff[256];
        printf("\nTesting reading and writing to fd: %d\n", fds[i]);

        write(fds[i], buffers[i], 256);
        
        // these next 2 lines are crucial so that the buffer that is written into fd can be flushed
        fds[i] = close(fds[i]);
        fds[i] = open(filenames[i]);

        // read new data
        read(fds[i], tempBuff, 256);

        // write(fds[i], '\n', 1);
        write(fds[i], tempBuff, 256);

        if(strcmp(tempBuff, buffers[i]) != 0) {
            printf("Read and write test failed. Test buffers are not matching\n");
        }
        else {
            printf("Test buffers are matching.\n");
        }
    }
}


int main() {

    createTestFileNames();

    testCreat();
    printf("\nTesting reading and writing...\n");
    createRandomBuffers();
    testReadAndWrite();
    testUnlink();
    testClose();

    printf("\nTesting creat, open, close and unlink...\n");
    testCreat();
    testOpen();
    testClose();
    testOpen();
    testClose();
    testClose();
    testUnlink();
    testOpen();
    testCreat();
    testUnlink();

    testClose();

    
    printf("\nTesting creating long names...\n");
    testCreatLongName();
    printf("\nTesting opening long names...\n");
    testOpenLongName();
    printf("\nTesting deleting long names...\n");
    testUnlinkLongName();

    printf("\nTesting exit... (if no more messages, exit was successful)\n");

    exit(0);
    printf("If this is printed, exit(0) is not properly terminating the process.\n");

}
