#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>

#include "stb_image_write.h"
#include "doom_lib.h"

void print_bytes(const uint8_t *bytes, size_t length) {
    for (size_t i = 0; i < length; i++) {
        // %02x prints each byte as a 2-digit hex value
        printf("%02x ", bytes[i]);
    }
    printf("\n");
}

int save_frame_as_image(const char* output)
{
    byte* main_screen = DOOM_GetScreenBuffer();
    byte* palette = DOOM_GetPalette();

    int width = DOOM_GetScreenWidth();
    int height = DOOM_GetScreenHeight();
    int channels = 3;

    unsigned char* rgb_data = (unsigned char*)malloc(width * height * 3);
    for (int i = 0; i < width * height; i++)
    {
        byte index = main_screen[i];
        rgb_data[i * 3] = palette[index * 3]; // r
        rgb_data[i * 3 + 1] = palette[index * 3 + 1]; // g
        rgb_data[i * 3 + 2] = palette[index * 3 + 2]; // b
    }
    
    int image = stbi_write_png(output, width, height, channels, rgb_data, width * channels);
    return image;
}

int main(int argc, char** argv)
{
    printf("Doom Sandbox\n");

    int wad_found = 0;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-iwad") == 0 && i + 1 < argc) {
            wad_found = 1;
            printf("Using IWAD: %s\n", argv[i + 1]);
            break;
        }
    }

    if (!wad_found) {
        printf("\nUsage: %s -iwad <path_to_doom.wad> -warp <ep> <map>\n", argv[0]);
        printf("\nExample:\n");
        printf("  %s -iwad doom.wad -warp 1 1\n", argv[0]);
        return 1;
    }

    printf("\nInitializing Doom engine...\n");
    DOOM_INIT(argc, argv);

    if (DOOM_GetScreenBuffer() == NULL)
    {
        printf("ERROR: Screen buffer not allocated!\n");
        return 1;
    }

    int wait_tics = 0;
    while (!DOOM_IsPlayerReady() && wait_tics < 35) {
        DOOM_RUNTICK();
        wait_tics++;
    }

    // if (!DOOM_IsPlayerReady())
    // {
    //     printf("ERROR: Player did not spawn\n");
    //     return 1;
    // }
    
    // Running 1 tick and printing the buffer
    DOOM_RUNTICK();
    DOOM_RENDERFRAME();

    if (save_frame_as_image("./image.png") == 0)
    {
        printf("ERROR: Failed to export frame");
    }
    

    return 0;
}
