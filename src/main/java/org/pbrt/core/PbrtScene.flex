package org.pbrt.core;

%%

%public
%integer
%function yylex
%char
%scanerror java.lang.Error

%{
    public Object getLVal () { return null; }
    public void yyerror (String msg) {
        Error.Error(msg);
    }
%}

LINE_COMMENT = #[^\n]*
DIGIT = [0-9]
FLOAT_NUMBER = [-+]*{DIGIT}*\.{DIGIT}*
INTEGER_NUMBER = [-+]*{DIGIT}+
STRING_LITERAL = \"([^\\\"]|\\[^])*(\")
WHITE_SPACE = [ \r\n\t]+

%%

"Accelerator"		{ return Parser.ACCELERATOR; }
"ActiveTransform"	{ return Parser.ACTIVETRANSFORM; }
"All"				{ return Parser.ALL; }
"AreaLightSource"	{ return Parser.AREALIGHTSOURCE; }
"AttributeBegin" 	{ return Parser.ATTRIBUTEBEGIN; }
"AttributeEnd" 		{ return Parser.ATTRIBUTEEND; }
"Camera"			{ return Parser.CAMERA; }
"ConcatTransform" 	{ return Parser.CONCATTRANSFORM; }
"CoordinateSystem"	{ return Parser.COORDINATESYSTEM; }
"CoordSysTransform"	{ return Parser.COORDSYSTRANSFORM; }
"EndTime"			{ return Parser.ENDTIME; }
"Film"				{ return Parser.FILM; }
"Identity"			{ return Parser.IDENTITY; }
"Include"			{ return Parser.INCLUDE; }
"LightSource" 		{ return Parser.LIGHTSOURCE; }
"LookAt"			{ return Parser.LOOKAT; }
"MakeNamedMedium"	{ return Parser.MAKENAMEDMEDIUM; }
"MakeNamedMaterial"	{ return Parser.MAKENAMEDMATERIAL; }
"Material"			{ return Parser.MATERIAL; }
"MediumInterface"   { return Parser.MEDIUMINTERFACE; }
"NamedMaterial"		{ return Parser.NAMEDMATERIAL; }
"ObjectBegin" 		{ return Parser.OBJECTBEGIN; }
"ObjectEnd" 		{ return Parser.OBJECTEND; }
"ObjectInstance" 	{ return Parser.OBJECTINSTANCE; }
"PixelFilter"		{ return Parser.PIXELFILTER; }
"ReverseOrientation" { return Parser.REVERSEORIENTATION; }
"Rotate"			{ return Parser.ROTATE; }
"Sampler"			{ return Parser.SAMPLER; }
"Scale" 			{ return Parser.SCALE; }
"Shape"				{ return Parser.SHAPE; }
"StartTime"			{ return Parser.STARTTIME; }
"Integrator"	    { return Parser.INTEGRATOR; }
"Texture"			{ return Parser.TEXTURE; }
"TransformBegin"	{ return Parser.TRANSFORMBEGIN; }
"TransformEnd"		{ return Parser.TRANSFORMEND; }
"TransformTimes"	{ return Parser.TRANSFORMTIMES; }
"Transform"			{ return Parser.TRANSFORM; }
"Translate"		    { return Parser.TRANSLATE; }
"WorldBegin" 		{ return Parser.WORLDBEGIN; }
"WorldEnd"			{ return Parser.WORLDEND; }

{FLOAT_NUMBER}      { return Parser.NUMBER; }
{INTEGER_NUMBER}    { return Parser.NUMBER; }

"["				    { return Parser.LBRACK; }
"]"				    { return Parser.RBRACK; }

{STRING_LITERAL}	{ return Parser.STRING; }

{WHITE_SPACE}       { }
{LINE_COMMENT}      { }

.				    { Error.Error("Illegal character: %c (0x%x)", yytext().charAt(0), (int)yytext().charAt(0)); }
