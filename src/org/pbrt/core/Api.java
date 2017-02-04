
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Api {
    // API Function Declarations
    public static void pbrtInit(Options opt) {

    }

    public static void pbrtCleanup() {

    }

    public static void pbrtIdentity() {

    }

    public static void pbrtTranslate(float dx, float dy, float dz) {

    }

    public static void pbrtRotate(float angle, float ax, float ay, float az) {

    }

    public static void pbrtScale(float sx, float sy, float sz) {

    }

    public static void pbrtLookAt(float ex, float ey, float ez, float lx, float ly, float lz, float ux, float uy, float uz) {

    }

    public static void pbrtConcatTransform(float[] transform) {

    }

    public static void pbrtTransform(float[] transform) {

    }

    public static void pbrtCoordinateSystem(String name) {

    }

    public static void pbrtCoordSysTransform(String name) {

    }

    public static void pbrtActiveTransformAll() {

    }

    public static void pbrtActiveTransformEndTime() {

    }

    public static void pbrtActiveTransformStartTime() {

    }

    public static void pbrtTransformTimes(float start, float end) {

    }

    public static void pbrtPixelFilter(String name, ParamSet params) {

    }

    public static void pbrtFilm(String type, ParamSet params) {

    }

    public static void pbrtSampler(String name, ParamSet params) {

    }

    public static void pbrtAccelerator(String name, ParamSet params) {
    }

    public static void pbrtIntegrator(String name, ParamSet params) {
    }

    public static void pbrtCamera(String name, ParamSet cameraParams) {
    }

    public static void pbrtMakeNamedMedium(String name, ParamSet params) {
    }

    public static void pbrtMediumInterface(String insideName, String outsideName) {
    }

    public static void pbrtWorldBegin() {
    }

    public static void pbrtAttributeBegin() {
    }

    public static void pbrtAttributeEnd() {
    }

    public static void pbrtTransformBegin() {
    }

    public static void pbrtTransformEnd() {
    }

    public static void pbrtTexture(String name, String type, String texname, ParamSet params) {
    }

    public static void pbrtMaterial(String name, ParamSet params) {
    }

    public static void pbrtMakeNamedMaterial(String name, ParamSet params) {
    }

    public static void pbrtNamedMaterial(String name) {
    }

    public static void pbrtLightSource(String name, ParamSet params) {
    }

    public static void pbrtAreaLightSource(String name, ParamSet params) {
    }

    public static void pbrtShape(String name, ParamSet params) {
    }

    public static void pbrtReverseOrientation() {
    }

    public static void pbrtObjectBegin(String name) {
    }

    public static void pbrtObjectEnd() {
    }

    public static void pbrtObjectInstance(String name) {
    }

    public static void pbrtWorldEnd() {
    }

}