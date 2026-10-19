#ifndef DOOM_SOUNDS
#define DOOM_SOUNDS

signed short* I_GetSoundBuffer(int* samples);
void I_UpdateSound(void);
void I_SubmitSound(void);
void I_ClearSoundBuffer(void);

#endif