package org.pbrt.core;

import com.intellij.lexer.*;
import org.pbrt.core.PbrtParser.Lexer;

%%

%class PbrtFlexLexer
%implements FlexLexer
%unicode
%function advance
%type Integer
%cup

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

"Accelerator"		{ return Lexer.ACCELERATOR; }
"ActiveTransform"	{ return Lexer.ACTIVETRANSFORM; }
"All"				{ return Lexer.ALL; }
"AreaLightSource"	{ return Lexer.AREALIGHTSOURCE; }
"AttributeBegin" 	{ return Lexer.ATTRIBUTEBEGIN; }
"AttributeEnd" 		{ return Lexer.ATTRIBUTEEND; }
"Camera"			{ return Lexer.CAMERA; }
"ConcatTransform" 	{ return Lexer.CONCATTRANSFORM; }
"CoordinateSystem"	{ return Lexer.COORDINATESYSTEM; }
"CoordSysTransform"	{ return Lexer.COORDSYSTRANSFORM; }
"EndTime"			{ return Lexer.ENDTIME; }
"Film"				{ return Lexer.FILM; }
"Identity"			{ return Lexer.IDENTITY; }
"Include"			{ return Lexer.INCLUDE; }
"LightSource" 		{ return Lexer.LIGHTSOURCE; }
"LookAt"			{ return Lexer.LOOKAT; }
"MakeNamedMedium"	{ return Lexer.MAKENAMEDMEDIUM; }
"MakeNamedMaterial"	{ return Lexer.MAKENAMEDMATERIAL; }
"Material"			{ return Lexer.MATERIAL; }
"MediumInterface"   { return Lexer.MEDIUMINTERFACE; }
"NamedMaterial"		{ return Lexer.NAMEDMATERIAL; }
"ObjectBegin" 		{ return Lexer.OBJECTBEGIN; }
"ObjectEnd" 		{ return Lexer.OBJECTEND; }
"ObjectInstance" 	{ return Lexer.OBJECTINSTANCE; }
"PixelFilter"		{ return Lexer.PIXELFILTER; }
"ReverseOrientation" { return Lexer.REVERSEORIENTATION; }
"Rotate"			{ return Lexer.ROTATE;	}
"Sampler"			{ return Lexer.SAMPLER; }
"Scale" 			{ return Lexer.SCALE; }
"Shape"				{ return Lexer.SHAPE; }
"StartTime"			{ return Lexer.STARTTIME; }
"Integrator"	    { return Lexer.INTEGRATOR; }
"Texture"			{ return Lexer.TEXTURE; }
"TransformBegin"	{ return Lexer.TRANSFORMBEGIN;	}
"TransformEnd"		{ return Lexer.TRANSFORMEND; }
"TransformTimes"	{ return Lexer.TRANSFORMTIMES; }
"Transform"			{ return Lexer.TRANSFORM; }
"Translate"		    { return Lexer.TRANSLATE; }
"WorldBegin" 		{ return Lexer.WORLDBEGIN; }
"WorldEnd"			{ return Lexer.WORLDEND; }


{FLOAT_NUMBER}      { return Lexer.NUMBER; }
{INTEGER_NUMBER}    { return Lexer.NUMBER; }

{IDENTIFIER}        { return Lexer.IDENTIFIER; }

"["				    { return Lexer.LBRACK; }
"]"				    { return Lexer.RBRACK; }

{STRING_LITERAL}	{ return Lexer.STRING; }


{WHITE_SPACE}       { }
{LINE_COMMENT}      { }

.				    { Error.Error("Illegal character: %c (0x%x)", yytext[0], (int)yytext[0])); }
