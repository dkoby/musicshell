/*
 *
 */
#include <jni.h>
#include <KissFFT.h> 
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>
#include "kiss_fft.h"

struct fft_t {
    int nsamples;
    kiss_fft_cfg kcfg;
    kiss_fft_cpx *in;
    kiss_fft_cpx *out;

    jbyte *inRaw;
    jint  *outRaw;
};

#define SAMPLE_WIDTH    2 /* bytes */
static struct fft_t fft;

/*
 *
 */
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    return JNI_VERSION_1_8;
}
#if 0
/*
 *
 */
JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
    fflush(stdout);
}
#endif

/*
 * Class:     mshell_jni_fft_KissFFT
 * Method:    
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_mshell_jni_fft_KissFFT_init
  (JNIEnv *env, jobject thisObject)
{
    jfieldID numSamplesID;
    jclass   thisClass;
    jint     numSamples;

    printf("FFT JNI Init\r\n");
    fflush(stdout);

    thisClass = (*env)->GetObjectClass(env, thisObject);
    numSamplesID = (*env)->GetFieldID(env, thisClass, "numSamples", "I");
    if ((*env)->ExceptionCheck(env))
        return;

    numSamples = (*env)->GetIntField(env, thisObject, numSamplesID);

    fft.kcfg    = kiss_fft_alloc(numSamples, 0 /* inverse_fft */, NULL, NULL);
    fft.in      = malloc(numSamples * sizeof(kiss_fft_cpx));
    fft.out     = malloc(numSamples * sizeof(kiss_fft_cpx));
    fft.inRaw   = malloc(numSamples * sizeof(jbyte) * SAMPLE_WIDTH);
    fft.outRaw  = malloc(numSamples * sizeof(jint));

    fft.nsamples = numSamples;
}
/*
 * Class:     mshell_jni_fft_KissFFT
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_mshell_jni_fft_KissFFT_destroy(JNIEnv *env, jobject thisObject)
{
    printf("FFT JNI Destroy\r\n");
    fflush(stdout);
    free(fft.kcfg);
    free(fft.in);
    free(fft.out);
    free(fft.inRaw);
    free(fft.outRaw);
}
/*
 * Class:     mshell_jni_fft_KissFFT
 * Method:    make
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_mshell_jni_fft_KissFFT_make(JNIEnv *env, jobject thisObject)
{
    jclass thisClass;
    jfieldID inputID;
    jfieldID outputID;
    jbyteArray inputArray;
    jintArray outputArray;

    thisClass = (*env)->GetObjectClass(env, thisObject);

    /*
     * Get data of input array (KissFFT.input).
     */
    inputID = (*env)->GetFieldID(env, thisClass, "input", "[B");
    if ((*env)->ExceptionCheck(env))
        return;
    inputArray = (jbyteArray)(*env)->GetObjectField(env, thisObject, inputID);
    if ((*env)->ExceptionCheck(env))
        return;
    (*env)->GetByteArrayRegion(env, inputArray, 0, (*env)->GetArrayLength(env, inputArray), fft.inRaw);
    if ((*env)->ExceptionCheck(env))
        return;

    /*
     * Make FFT of input. Fill output.
     */
    {
        int n;
        kiss_fft_cpx *pInKiss;
        kiss_fft_cpx *pOutKiss;
        int16_t *pInRaw;
        jint *pOutRaw;

        n       = fft.nsamples;
        pInKiss = fft.in;
        pInRaw  = (int16_t *)fft.inRaw;
        while (n--)
        {
//            pInKiss->r = *pInRaw + 32768 /* XXX */;
            pInKiss->r = *pInRaw + 65535;
            pInKiss->i = 0;
            
            pInKiss++;
            pInRaw++;
        }

        /* make FFT */
        kiss_fft(fft.kcfg, fft.in, fft.out);

        n        = fft.nsamples;
        pOutKiss = fft.out;
        pOutRaw  = fft.outRaw;
        while (n--)
        {
            *pOutRaw = (jint)sqrt(pOutKiss->r * pOutKiss->r + pOutKiss->i * pOutKiss->i);

            pOutRaw++;
            pOutKiss++;
        }
    }

    /*
     * Set data of output array (KissFFT.output).
     */
    outputID = (*env)->GetFieldID(env, thisClass, "output", "[I");
    if ((*env)->ExceptionCheck(env))
        return;
    outputArray = (jintArray)(*env)->GetObjectField(env, thisObject, outputID);
    if ((*env)->ExceptionCheck(env))
        return;

    /* synchronize access to result array */
    (*env)->MonitorEnter(env, thisObject);
    {
        /* copy raw result to output array */
        (*env)->SetIntArrayRegion(env, outputArray, 0, (*env)->GetArrayLength(env, outputArray), fft.outRaw);
        if ((*env)->ExceptionCheck(env))
            return;
    }
    (*env)->MonitorExit(env, thisObject);
    if ((*env)->ExceptionCheck(env))
    {
        /* NOTREACHED */
        return;
    }
}

