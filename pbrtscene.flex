package org.pbrt.lang.core.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;

%%

%class PbrtFlexLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType


%{
%}

IDENTIFIER = [a-zA-Z_][a-zA-Z0-9_]*
DIGIT = [0-9]
FLOAT_NUMBER = [-+]*{DIGIT}*\.{DIGIT}*
INTEGER_NUMBER = [-+]*{DIGIT}+
LINE_COMMENT = #[^\n]*
STRING_LITERAL = \" ( [^\\\"] | \\[^] )* ( \" | \\ )?
WHITE_SPACE = [ \r\n\t]+

%%

{WHITE_SPACE}       { return WHITE_SPACE; }

{LINE_COMMENT}      { return LINE_COMMENT; }

{FLOAT_NUMBER}      { return NUMBER; }
{INTEGER_NUMBER}    { return NUMBER; }

"["				    { return LBRACK }
"]"				    { return RBRACK }

"Accelerator"		{ return ACCELERATOR }
"ActiveTransform"	{ return ACTIVETRANSFORM }
"All"				{ return ALL }
"AreaLightSource"	{ return AREALIGHTSOURCE }
"AttributeBegin" 	{ return ATTRIBUTEBEGIN }
"AttributeEnd" 		{ return ATTRIBUTEEND }
"Camera"			{ return CAMERA }
"ConcatTransform" 	{ return CONCATTRANSFORM }
"CoordinateSystem"	{ return COORDINATESYSTEM }
"CoordSysTransform"	{ return COORDSYSTRANSFORM }
"EndTime"			{ return ENDTIME }
"Film"				{ return FILM }
"Identity"			{ return IDENTITY }
"Include"			{ return INCLUDE }
"LightSource" 		{ return LIGHTSOURCE }
"LookAt"			{ return LOOKAT }
"MakeNamedMaterial"	{ return MAKENAMEDMATERIAL }
"Material"			{ return MATERIAL }
"NamedMaterial"		{ return NAMEDMATERIAL }
"ObjectBegin" 		{ return OBJECTBEGIN }
"ObjectEnd" 		{ return OBJECTEND }
"ObjectInstance" 	{ return OBJECTINSTANCE }
"PixelFilter"		{ return PIXELFILTER }
"Renderer"			{ return RENDERER }
"ReverseOrientation" { return REVERSEORIENTATION }
"Rotate"			{ return ROTATE	}
"Sampler"			{ return SAMPLER }
"Scale" 			{ return SCALE }
"Shape"				{ return SHAPE }
"StartTime"			{ return STARTTIME }
"SurfaceIntegrator"	{ return SURFACEINTEGRATOR }
"Texture"			{ return TEXTURE }
"TransformBegin"	{ return TRANSFORMBEGIN	}
"TransformEnd"		{ return TRANSFORMEND }
"TransformTimes"	{ return TRANSFORMTIMES }
"Transform"			{ return TRANSFORM }
"Translate"		    { return TRANSLATE }
"Volume"			{ return VOLUME }
"VolumeIntegrator"	{ return VOLUMEINTEGRATOR }
"WorldBegin" 		{ return WORLDBEGIN }
"WorldEnd"			{ return WORLDEND }


{IDENTIFIER}        { return IDENTIFIER; }
{STRING_LITERAL}	{ return STRING }

.				    { return UNKNOWN; }
