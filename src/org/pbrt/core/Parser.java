
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.ArrayList;
import java.util.Objects;

public class Parser {

    public static final int STRING = 10;
    public static final int NUMBER = 11;

    public static final int LBRACK = 20;
    public static final int RBRACK = 21;
    public static final int ALL = 22;
    public static final int ENDTIME = 23;
    public static final int STARTTIME = 24;

    private static final int FIRST_COMMAND = 40;
    public static final int ACCELERATOR = 46;
    public static final int ACTIVETRANSFORM = 47;
    public static final int AREALIGHTSOURCE = 49;
    public static final int ATTRIBUTEBEGIN = 50;
    public static final int ATTRIBUTEEND = 51;
    public static final int CAMERA = 52;
    public static final int CONCATTRANSFORM = 53;
    public static final int COORDINATESYSTEM = 54;
    public static final int COORDSYSTRANSFORM = 55;
    public static final int FILM = 57;
    public static final int IDENTITY = 58;
    public static final int INCLUDE = 59;
    public static final int LIGHTSOURCE = 60;
    public static final int LOOKAT = 61;
    public static final int MAKENAMEDMATERIAL = 62;
    public static final int MAKENAMEDMEDIUM = 63;
    public static final int MEDIUMINTERFACE = 64;
    public static final int MATERIAL = 65;
    public static final int NAMEDMATERIAL = 66;
    public static final int OBJECTBEGIN = 67;
    public static final int OBJECTEND = 68;
    public static final int OBJECTINSTANCE = 69;
    public static final int PIXELFILTER = 70;
    public static final int REVERSEORIENTATION = 71;
    public static final int ROTATE = 72;
    public static final int SAMPLER = 73;
    public static final int SCALE = 74;
    public static final int SHAPE = 75;
    public static final int INTEGRATOR = 77;
    public static final int TEXTURE = 78;
    public static final int TRANSFORMBEGIN = 79;
    public static final int TRANSFORMEND = 80;
    public static final int TRANSFORMTIMES = 81;
    public static final int TRANSFORM = 82;
    public static final int TRANSLATE = 83;
    public static final int WORLDBEGIN = 84;
    public static final int WORLDEND = 85;

    public static boolean ParseFile(String filename) {
        try {
            Yylex scanner = new Yylex( new java.io.FileReader(filename) );
/*
            int token = scanner.yylex();
            while ( token != -1 ) {
                token = scanner.yylex();
                System.out.printf("Token: %s\n", scanner.yytext());
            }
*/
            Parser p = new Parser(scanner);
            p.parse();
        }
        catch (Exception e) {
            Error.Error("Failed to parse file, %s. Error: %s", filename, e.toString());
            e.printStackTrace();
        }
        return true;
    }

    public static class PbrtParameter {

        public String type;
        public String name;
        public Object value;

        public PbrtParameter(String descriptor, Object value) {
            if (descriptor != null) {
                String[] tokens = descriptor.split(" ");
                assert tokens.length == 2;
                this.type = tokens[0];
                this.name = tokens[1];
            }
            this.value = value;
        }
    }

    private ParamSet createParamSet(ArrayList<PbrtParameter> paramlist) {
        if (paramlist == null) return null;

        ParamSet pset = new ParamSet();
        for (PbrtParameter param : paramlist) {
            if (Objects.equals(param.type, "integer")) {
                if (param.value instanceof ArrayList) {
                    // parser converts all values to float, convert back to int
                    ArrayList<Float> flist = (ArrayList<Float>)param.value;
                    Integer[] ivalue = new Integer[flist.size()];
                    for (int i = 0; i < flist.size(); i++) {
                        ivalue[i] = (int)Math.floor(flist.get(i));
                    }
                    pset.AddInt(param.name, ivalue);
                }
                else {
                    Error.Error("Unexpected value array type for 'integer' parameter.  Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "bool")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Boolean> blist = (ArrayList<Boolean>)param.value;
                    Boolean[] barray = new Boolean[blist.size()];
                    for (int i = 0; i < barray.length; i++) {
                        barray[i] = blist.get(i);
                    }
                    pset.AddBoolean(param.name, barray);
                }
                else {
                    Error.Error("Unexpected value array type for 'bool' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "float")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> flist = (ArrayList<Float>)param.value;
                    Float[] farray = new Float[flist.size()];
                    for (int i = 0; i < farray.length; i++) {
                        farray[i] = flist.get(i);
                    }
                    pset.AddFloat(param.name, farray);
                }
                else {
                    Error.Error("Unexpected value array type for 'float' parameter.  Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "point2")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> pvalues = (ArrayList<Float>)param.value;
                    int nItems = pvalues.size();
                    if (nItems % 2 == 0) {
                        Point2f[] points = new Point2f[nItems/2];
                        for (int i = 0; i < points.length; i++) {
                            points[i] = new Point2f(pvalues.get(i*2), pvalues.get(i*2+1));
                        }
                        pset.AddPoint2f(param.name, points);
                    }
                    else {
                        Error.Error("Length of 'point2' parameter list must be a factor of 2.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'point2' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "vector2")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> vvalues = (ArrayList<Float>)param.value;
                    int nItems = vvalues.size();
                    if (nItems % 2 == 0) {
                        Vector2f[] vectors = new Vector2f[nItems/2];
                        for (int i = 0; i < vectors.length; i++) {
                            vectors[i] = new Vector2f(vvalues.get(i*2), vvalues.get(i*2+1));
                        }
                        pset.AddVector2f(param.name, vectors);
                    }
                    else {
                        Error.Error("Length of 'vector2' parameter list must be a factor of 2.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'vector2' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if ((Objects.equals(param.type, "point3")) || (Objects.equals(param.type, "point"))) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> pvalues = (ArrayList<Float>)param.value;
                    int nItems = pvalues.size();
                    if (nItems % 3 == 0) {
                        Point3f[] points = new Point3f[nItems/3];
                        for (int i = 0; i < points.length; i++) {
                            points[i] = new Point3f(pvalues.get(i*3), pvalues.get(i*3+1), pvalues.get(i*3+2));
                        }
                        pset.AddPoint3f(param.name, points);
                    }
                    else {
                        Error.Error("Length of 'point3' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'point3' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "vector3")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> vvalues = (ArrayList<Float>)param.value;
                    int nItems = vvalues.size();
                    if (nItems % 3 == 0) {
                        Vector3f[] vectors = new Vector3f[nItems/3];
                        for (int i = 0; i < vectors.length; i++) {
                            vectors[i] = new Vector3f(vvalues.get(i*3), vvalues.get(i*3+1), vvalues.get(i*3+2));
                        }
                        pset.AddVector3f(param.name, vectors);
                    }
                    else {
                        Error.Error("Length of 'vector3' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'vector3' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "normal")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> nvalues = (ArrayList<Float>)param.value;
                    int nItems = nvalues.size();
                    if (nItems % 3 == 0) {
                        Normal3f[] normals = new Normal3f[nItems/3];
                        for (int i = 0; i < normals.length; i++) {
                            normals[i] = new Normal3f(nvalues.get(i*3), nvalues.get(i*3+1), nvalues.get(i*3+2));
                        }
                        pset.AddNormal3f(param.name, normals);
                    }
                    else {
                        Error.Error("Length of 'normal' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'normal' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "rgb") || Objects.equals(param.type, "color")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> cvalues = (ArrayList<Float>)param.value;
                    if (cvalues.size() % 3 == 0) {
                        Float[] carray = new Float[cvalues.size()];
                        for (int i = 0; i < carray.length; i++) {
                            carray[i] = cvalues.get(i);
                        }
                        pset.AddRGBSpectrum(param.name, carray);
                    }
                    else {
                        Error.Error("Length of 'rgb' or 'color' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'rgb' or 'color' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "xyz")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> xvalues = (ArrayList<Float>)param.value;
                    if (xvalues.size() % 3 == 0) {
                        Float[] xarray = new Float[xvalues.size()];
                        for (int i = 0; i < xarray.length; i++) {
                            xarray[i] = xvalues.get(i);
                        }
                        pset.AddXYZSpectrum(param.name, xarray);
                    }
                    else {
                        Error.Error("Length of 'xyz' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'xyz' parameter'. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "blackbody")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> values = (ArrayList<Float>)param.value;
                    Float[] barray = new Float[values.size()];
                    for (int i = 0; i < barray.length; i++) {
                        barray[i] = values.get(i);
                    }
                    pset.AddBlackbodySpectrum(param.name, barray);
                }
                else {
                    Error.Error("Unexpected value array type for 'blackbody' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "spectrum")) {
                if (param.value instanceof ArrayList) {
                    ArrayList<Float> values = (ArrayList<Float>)param.value;
                    Float[] sarray = new Float[values.size()];
                    for (int i = 0; i < sarray.length; i++) {
                        sarray[i] = values.get(i);
                    }
                    pset.AddSampledSpectrum(param.name, sarray);
                }
                else if (param.value instanceof String[]) {
                    pset.AddSampledSpectrumFiles(param.name, (String[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'spectrum' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "string")) {
                if (param.value instanceof String) {
                    String[] strings = { (String)param.value };
                    pset.AddString(param.name, strings);
                }
                else if (param.value instanceof ArrayList) {
                    ArrayList<String> slist = (ArrayList<String>)param.value;
                    String[] strings = new String[slist.size()];
                    for (int i= 0; i < strings.length; i++) {
                        strings[i] = slist.get(i);
                    }
                    pset.AddString(param.name, strings);
                }
                else {
                    Error.Error("Unexpected value array type for 'string' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else if (Objects.equals(param.type, "texture")) {
                if (param.value instanceof String) {
                    String[] strings = { (String)param.value };
                    pset.AddTexture(param.name, (String)param.value);
                }
                else {
                    Error.Error("Unexpected value type for 'texture' parameter. Got %s.\n", param.value.getClass().toString());
                }
            }
            else {
                Error.Error("Unknown parameter type: %s\n", param.type);
            }
        }
        return pset;
    }

    private Yylex scanner;

    public Parser(Yylex scanner) {
        this.scanner = scanner;
    }

    private class TokenValue {
        public int token;
        public String value;
        public TokenValue(int token, String value) {
            this.token = token;
            this.value = value;
        }
    }
    private class CommandTokens {
        public ArrayList<TokenValue> command = new ArrayList<>(2);
    }

    public void parse() {


        try {
            int token = 0;
            CommandTokens currentCommand = null;
            while (token != -1) {
                token = scanner.yylex();
                if (isCommand(token)) {
                    if (currentCommand != null) {
                        processCommand(currentCommand);
                    }
                    // start a new command
                    currentCommand = new CommandTokens();
                }
                currentCommand.command.add(new TokenValue(token, scanner.yytext()));
            }
        }
        catch (Exception e) {
            Error.Error("Failed to parse file. Error: %s", e.toString());
            e.printStackTrace();
        }
    }

    private void processCommand(CommandTokens currentCommand) {
        assert(currentCommand != null);
        int commandToken = currentCommand.command.get(0).token;
        assert(isCommand(commandToken));
        System.out.printf("Command: %s  Num Args: %d\n", currentCommand.command.get(0).value, currentCommand.command.size()-1);
        switch (commandToken) {
            case ACCELERATOR:
                parseAccelerator(currentCommand.command);
                break;
            case ACTIVETRANSFORM:
                parseActiveTransform(currentCommand.command);
                break;
            case AREALIGHTSOURCE:
                parseAreaLightSource(currentCommand.command);
                break;
            case ATTRIBUTEBEGIN:
                parseAttributeBegin(currentCommand.command);
                break;
            case ATTRIBUTEEND:
                parseAttributeEnd(currentCommand.command);
                break;
            case CAMERA:
                parseCamera(currentCommand.command);
                break;
            case CONCATTRANSFORM:
                parseConcatTransform(currentCommand.command);
                break;
            case COORDINATESYSTEM:
                parseCoordinateSystem(currentCommand.command);
                break;
            case COORDSYSTRANSFORM:
                parseCoordSysTransform(currentCommand.command);
                break;
            case FILM:
                parseFilm(currentCommand.command);
                break;
            case IDENTITY:
                parseIdentity(currentCommand.command);
                break;
            case INCLUDE:
                parseInclude(currentCommand.command);
                break;
            case LIGHTSOURCE:
                parseLightSource(currentCommand.command);
                break;
            case LOOKAT:
                parseLookAt(currentCommand.command);
                break;
            case MAKENAMEDMATERIAL:
                parseMakeNamedMaterial(currentCommand.command);
                break;
            case MAKENAMEDMEDIUM:
                parseMakeNamedMedium(currentCommand.command);
                break;
            case MEDIUMINTERFACE:
                parseMediumInterface(currentCommand.command);
                break;
            case MATERIAL:
                parseMaterial(currentCommand.command);
                break;
            case NAMEDMATERIAL:
                parseNamedMaterial(currentCommand.command);
                break;
            case OBJECTBEGIN:
                parseObjectBegin(currentCommand.command);
                break;
            case OBJECTEND:
                parseObjectEnd(currentCommand.command);
                break;
            case OBJECTINSTANCE:
                parseObjectInstance(currentCommand.command);
                break;
            case PIXELFILTER:
                parsePixelFilter(currentCommand.command);
                break;
            case REVERSEORIENTATION:
                parseReverseOrientation(currentCommand.command);
                break;
            case ROTATE:
                parseRotate(currentCommand.command);
                break;
            case SAMPLER:
                parseSampler(currentCommand.command);
                break;
            case SCALE:
                parseScale(currentCommand.command);
                break;
            case SHAPE:
                parseShape(currentCommand.command);
                break;
            case INTEGRATOR:
                parseIntegrator(currentCommand.command);
                break;
            case TEXTURE:
                parseTexture(currentCommand.command);
                break;
            case TRANSFORMBEGIN:
                parseTransformBegin(currentCommand.command);
                break;
            case TRANSFORMEND:
                parseTransformEnd(currentCommand.command);
                break;
            case TRANSFORMTIMES:
                parseTransformTimes(currentCommand.command);
                break;
            case TRANSFORM:
                parserTransform(currentCommand.command);
                break;
            case TRANSLATE:
                parseTranslate(currentCommand.command);
                break;
            case WORLDBEGIN:
                parseWorldBegin(currentCommand.command);
                break;
            case WORLDEND:
                parseWorldEnd(currentCommand.command);
                break;
            default:
                break;

        }
    }

    private boolean isCommand(int token) {
        return (token >= FIRST_COMMAND);
    }

    // ACCELERATOR STRING param_list
    private void parseAccelerator(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtAccelerator(name, createParamSet(params));
    }

    // ACTIVETRANSFORM ALL|ENDTIME|STARTTIME
    private void parseActiveTransform(ArrayList<TokenValue> command) {
        assert(command.size() == 2);
        if (command.get(1).token == ALL) {
            Api.pbrtActiveTransformAll();
        }
        else if (command.get(1).token == ENDTIME) {
            Api.pbrtActiveTransformEndTime();
        }
        else if (command.get(1).token == STARTTIME) {
            Api.pbrtActiveTransformStartTime();
        }
        else {
            assert(false);
        }
    }

    // AREALIGHTSOURCE STRING param_list
    private void parseAreaLightSource(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtAreaLightSource(name, createParamSet(params));
    }

    // ATTRIBUTEBEGIN
    private void parseAttributeBegin(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtAttributeBegin();
    }

    // ATTRIBUTEEND
    private void parseAttributeEnd(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtAttributeEnd();
    }

    // CAMERA STRING param_list
    private void parseCamera(ArrayList<TokenValue> command) {
        assert (command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtCamera(name, createParamSet(params));
    }

    // CONCATTRANSFORM number_array
    private void parseConcatTransform(ArrayList<TokenValue> command) {
    }

    // COORDINATESYSTEM STRING
    private void parseCoordinateSystem(ArrayList<TokenValue> command) {
        assert(command.size() == 2);
        Api.pbrtCoordinateSystem(command.get(1).value);
    }

    // COORDSYSTRANSFORM STRING
    private void parseCoordSysTransform(ArrayList<TokenValue> command) {
        assert(command.size() == 2);
        Api.pbrtCoordSysTransform(command.get(1).value);
    }

    // FILM STRING param_list
    private void parseFilm(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtFilm(name, createParamSet(params));
    }

    // IDENTITY
    private void parseIdentity(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtIdentity();
    }

    // INCLUDE STRING
    private void parseInclude(ArrayList<TokenValue> command) {
        assert(command.size() == 2);
        String filename = command.get(1).value;
        try {
            scanner.yyreset(new java.io.FileReader(filename));
        }
        catch (Exception e) {
            Error.Error("Failed to parse included file, %s. Error: %s", filename, e.toString());
            e.printStackTrace();
        }
    }

    // LIGHTSOURCE STRING param_list
    private void parseLightSource(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtLightSource(name, createParamSet(params));
    }

    // LOOKAT NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER
    private void parseLookAt(ArrayList<TokenValue> command) {
        assert(command.size() == 10);

        float ex = Float.parseFloat(command.get(1).value);
        float ey = Float.parseFloat(command.get(2).value);
        float ez = Float.parseFloat(command.get(3).value);
        float lx = Float.parseFloat(command.get(4).value);
        float ly = Float.parseFloat(command.get(5).value);
        float lz = Float.parseFloat(command.get(6).value);
        float ux = Float.parseFloat(command.get(7).value);
        float uy = Float.parseFloat(command.get(8).value);
        float uz = Float.parseFloat(command.get(9).value);

        Api.pbrtLookAt(ex, ey, ez, lx, ly, lz, ux, uy, uz);
    }

    // MAKENAMEDMATERIAL STRING param_list
    private void parseMakeNamedMaterial(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtMakeNamedMaterial(name, createParamSet(params));
    }

    // MAKENAMEDMEDIUM STRING param_list
    private void parseMakeNamedMedium(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtMakeNamedMedium(name, createParamSet(params));
    }

    // MATERIAL STRING param_list
    private void parseMaterial(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtMaterial(name, createParamSet(params));
    }

    // MEDIUMINTERFACE STRING <STRING>
    private void parseMediumInterface(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String med0 = command.get(1).value;
        String med1 = med0;
        if (command.size() == 3) {
            med1 = command.get(2).value;
        }

        Api.pbrtMediumInterface(med0, med1);
    }

    // NAMEMATERIAL STRING
    private void parseNamedMaterial(ArrayList<TokenValue> command) {
        assert(command.size() == 2);
        Api.pbrtNamedMaterial(command.get(1).value);
    }

    // OBJECTBEGIN STRING
    private void parseObjectBegin(ArrayList<TokenValue> command) {
        assert(command.size() == 2);
        Api.pbrtObjectBegin(command.get(1).value);
    }

    // OBJECTEND
    private void parseObjectEnd(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtObjectEnd();
    }

    // OBJECTINSTANCE STRING
    private void parseObjectInstance(ArrayList<TokenValue> command) {
        assert(command.size() == 2);
        Api.pbrtObjectInstance(command.get(1).value);
    }

    // PIXELFILTER STRING param_list
    private void parsePixelFilter(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtPixelFilter(name, createParamSet(params));
    }

    // REVERSEORIENTATION
    private void parseReverseOrientation(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtReverseOrientation();
    }

    // ROTATE NUMBER NUMBER NUMBER NUMBER
    private void parseRotate(ArrayList<TokenValue> command) {
        assert(command.size() == 5);

        float angle = Float.parseFloat(command.get(1).value);
        float xa = Float.parseFloat(command.get(2).value);
        float ya = Float.parseFloat(command.get(3).value);
        float za = Float.parseFloat(command.get(4).value);
        Api.pbrtRotate(angle, xa, ya, za);
    }

    // SAMPLER STRING param_list
    private void parseSampler(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtSampler(name, createParamSet(params));
    }

    // SCALE NUMBER NUMBER NUMBER
    private void parseScale(ArrayList<TokenValue> command) {
        assert(command.size() == 4);

        float sx = Float.parseFloat(command.get(1).value);
        float sy = Float.parseFloat(command.get(2).value);
        float sz = Float.parseFloat(command.get(3).value);
        Api.pbrtScale(sx, sy, sz);
    }

    // SHAPE STRING param_list
    private void parseShape(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtShape(name, createParamSet(params));
    }

    // INTEGRATOR STRING param_list
    private void parseIntegrator(ArrayList<TokenValue> command) {
        assert(command.size() >= 2);
        String name = command.get(1).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 2);

        Api.pbrtIntegrator(name, createParamSet(params));
    }

    // TEXTURE STRING STRING STRING param_list
    private void parseTexture(ArrayList<TokenValue> command) {
        assert(command.size() >= 4);
        String name = command.get(1).value;
        String type = command.get(2).value;
        String texname = command.get(3).value;
        ArrayList<PbrtParameter> params = extractParamList(command, 4);

        Api.pbrtTexture(name, type, texname, createParamSet(params));
    }

    // TRANSFORMBEGIN
    private void parseTransformBegin(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtTransformBegin();
    }

    // TRANSFORMEND
    private void parseTransformEnd(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtTransformEnd();
    }

    // TRANSFORMTIMES NUMBER NUMBER
    private void parseTransformTimes(ArrayList<TokenValue> command) {
        assert(command.size() == 3);

        float t0 = Float.parseFloat(command.get(1).value);
        float t1 = Float.parseFloat(command.get(2).value);
        Api.pbrtTransformTimes(t0, t1);
    }

    // TRANSFORM number_array
    private void parserTransform(ArrayList<TokenValue> command) {

    }

    // TRANSLATE NUMBER NUMBER NUMBER
    private void parseTranslate(ArrayList<TokenValue> command) {
        assert(command.size() == 4);

        float dx = Float.parseFloat(command.get(1).value);
        float dy = Float.parseFloat(command.get(2).value);
        float dz = Float.parseFloat(command.get(3).value);
        Api.pbrtTranslate(dx, dy, dz);
    }

    // WORLDBEGIN
    private void parseWorldBegin(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtWorldBegin();
    }

    // WORLDEND
    private void parseWorldEnd(ArrayList<TokenValue> command) {
        assert(command.size() == 1);
        Api.pbrtWorldEnd();
    }

    private ArrayList<PbrtParameter> extractParamList(ArrayList<TokenValue> command, int firstParam) {
        if (command.size() < firstParam) return null;

        ArrayList<Object> arrayList;
        ArrayList<PbrtParameter> params = new ArrayList<>(2);
        for (int i = firstParam; i < command.size(); i++) {
            assert(command.get(i).token == STRING);
            String paramName = command.get(i).value.substring(1, command.get(i).value.length()-1);
            i++;

            if (command.get(i).token == STRING) {
                params.add(new PbrtParameter(paramName, command.get(i).value));
            }
            else if (command.get(i).token == NUMBER) {
                params.add(new PbrtParameter(paramName, Float.parseFloat(command.get(i).value)));
            }
            else if (command.get(i).token == LBRACK) {
                // starting a list [...]
                arrayList = new ArrayList<>(2);
                i++;
                while (command.get(i).token != RBRACK) {
                    if (command.get(i).token == STRING) {
                        arrayList.add(new String(command.get(i).value));
                    }
                    else if (command.get(i).token == NUMBER) {
                        arrayList.add(Float.parseFloat(command.get(i).value));
                    }
                    else {
                        assert(false);
                    }
                    i++;
                }
                params.add(new PbrtParameter(paramName, arrayList));
            }
        }
        return params;
    }
}