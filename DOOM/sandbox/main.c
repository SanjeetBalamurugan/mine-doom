#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>

#ifdef _WIN32
#include <direct.h>
#define MKDIR(p) _mkdir(p)
#else
#include <sys/stat.h>
#include <sys/types.h>
#define MKDIR(p) mkdir(p, 0755)
#endif

#include "stb_image_write.h"
#include "doom_lib.h"
#include "doom_sounds.h"

#define WAV_RATE 11025
#define RUN_TICS (60 * 35)

void print_bytes(const uint8_t *bytes, size_t length) {
    for (size_t i = 0; i < length; i++) {
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
        rgb_data[i * 3] = palette[index * 3];
        rgb_data[i * 3 + 1] = palette[index * 3 + 1];
        rgb_data[i * 3 + 2] = palette[index * 3 + 2];
    }

    int image = stbi_write_png(output, width, height, channels, rgb_data, width * channels);
    free(rgb_data);
    return image;
}

static void put16(FILE* f, uint16_t v)
{
    fputc(v & 255, f);
    fputc((v >> 8) & 255, f);
}

static void put32(FILE* f, uint32_t v)
{
    put16(f, v & 65535);
    put16(f, v >> 16);
}

int save_sound_as_wav(const char* output, const int16_t* data, int samples)
{
    FILE* f = fopen(output, "wb");
    if (!f) return 0;

    uint32_t bytes = (uint32_t)samples * 2;
    fwrite("RIFF", 1, 4, f);
    put32(f, 36 + bytes);
    fwrite("WAVEfmt ", 1, 8, f);
    put32(f, 16);
    put16(f, 1);
    put16(f, 2);
    put32(f, WAV_RATE);
    put32(f, WAV_RATE * 4);
    put16(f, 4);
    put16(f, 16);
    fwrite("data", 1, 4, f);
    put32(f, bytes);
    for (int i = 0; i < samples; i++)
        put16(f, (uint16_t)data[i]);

    fclose(f);
    return 1;
}

static void step_tic(void)
{
    int before, after;
    I_GetSoundBuffer(&before);
    DOOM_RUNTICK();
    I_GetSoundBuffer(&after);
    if (after == before)
    {
        I_UpdateSound();
        I_SubmitSound();
    }
}

static void set_key(int* state, int want, int key)
{
    if (want && !*state) DOOM_KeyDown(key);
    else if (!want && *state) DOOM_KeyUp(key);
    *state = want;
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

    MKDIR("export");
    MKDIR("export/frames");

    printf("\nInitializing Doom engine...\n");
    DOOM_INIT(argc, argv);

    if (DOOM_GetScreenBuffer() == NULL)
    {
        printf("ERROR: Screen buffer not allocated!\n");
        return 1;
    }

    int wait_tics = 0;
    while (!DOOM_IsPlayerReady() && wait_tics < 350) {
        if (wait_tics % 10 == 0) DOOM_SendKeyPress(DOOM_KEY_ENTER);
        step_tic();
        wait_tics++;
    }

    if (!DOOM_IsPlayerReady())
    {
        printf("ERROR: Player did not spawn\n");
        return 1;
    }

    int sound_start;
    I_GetSoundBuffer(&sound_start);

    int fwd = 0, turnr = 0, turnl = 0, fire = 0;
    char path[128];

    for (int t = 0; t < RUN_TICS; t++)
    {
        int phase = (t / 70) % 4;
        set_key(&fwd, 1, DOOM_KEY_UPARROW);
        set_key(&turnr, phase == 1, DOOM_KEY_RIGHTARROW);
        set_key(&turnl, phase == 3, DOOM_KEY_LEFTARROW);
        set_key(&fire, (t % 35) < 10, DOOM_KEY_RCTRL);

        step_tic();
        DOOM_RENDERFRAME();

        snprintf(path, sizeof(path), "export/frames/frame_%04d.png", t);
        if (save_frame_as_image(path) == 0)
        {
            printf("ERROR: Failed to export %s\n", path);
            return 1;
        }
    }

    int total;
    int16_t* snd = I_GetSoundBuffer(&total);
    if (!snd || total <= sound_start)
    {
        printf("ERROR: No sound captured\n");
        return 1;
    }

    if (save_sound_as_wav("export/finalsound.wav", snd + sound_start, total - sound_start) == 0)
    {
        printf("ERROR: Failed to export sound\n");
        return 1;
    }

    printf("Exported %d frames and %d sound samples\n", RUN_TICS, (total - sound_start) / 2);
    return 0;
}