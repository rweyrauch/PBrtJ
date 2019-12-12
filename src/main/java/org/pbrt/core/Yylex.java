/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

/* The following code was generated by JFlex 1.4.3 on 2/15/17 5:15 PM */

package org.pbrt.core;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.4.3
 * on 2/15/17 5:15 PM from the specification file
 * <tt>PbrtScene.flex</tt>
 */
public class Yylex {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0, 0
  };

  /** 
   * Translates characters to character classes
   */
  private static final String ZZ_CMAP_PACKED = 
    "\11\0\1\10\1\2\2\0\1\10\22\0\1\10\1\0\1\6\1\1"+
    "\7\0\1\4\1\0\1\4\1\5\1\0\12\3\7\0\1\11\1\36"+
    "\1\41\1\0\1\37\1\43\2\0\1\44\2\0\1\30\1\46\1\47"+
    "\1\50\1\52\1\0\1\54\1\33\1\23\2\0\1\56\3\0\1\57"+
    "\1\7\1\60\3\0\1\16\1\35\1\12\1\40\1\13\1\26\1\31"+
    "\1\32\1\21\1\51\1\45\1\14\1\27\1\24\1\20\1\55\1\0"+
    "\1\15\1\25\1\17\1\34\1\22\1\0\1\53\1\42\uff86\0";

  /** 
   * Translates characters to character classes
   */
  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\1\0\1\1\2\2\1\3\1\1\1\3\17\1\1\4"+
    "\1\5\2\0\1\6\35\0\1\7\53\0\1\10\24\0"+
    "\1\11\2\0\1\12\27\0\1\13\4\0\1\14\15\0"+
    "\1\15\6\0\1\16\3\0\1\17\1\0\1\20\3\0"+
    "\1\21\31\0\1\22\1\0\1\23\10\0\1\24\4\0"+
    "\1\25\1\26\1\0\1\27\10\0\1\30\20\0\1\31"+
    "\7\0\1\32\1\33\7\0\1\34\7\0\1\35\1\0"+
    "\1\36\4\0\1\37\2\0\1\40\24\0\1\41\4\0"+
    "\1\42\1\43\1\44\6\0\1\45\1\0\1\46\1\47"+
    "\2\0\1\50\1\51\1\52\2\0\1\53\3\0\1\54"+
    "\1\55\1\0\1\56";

  private static int [] zzUnpackAction() {
    int [] result = new int[344];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\61\0\142\0\223\0\304\0\365\0\u0126\0\u0157"+
    "\0\u0188\0\u01b9\0\u01ea\0\u021b\0\u024c\0\u027d\0\u02ae\0\u02df"+
    "\0\u0310\0\u0341\0\u0372\0\u03a3\0\u03d4\0\u0405\0\61\0\61"+
    "\0\365\0\u0157\0\61\0\u0436\0\u0467\0\u0498\0\u04c9\0\u04fa"+
    "\0\u052b\0\u055c\0\u058d\0\u05be\0\u05ef\0\u0620\0\u0651\0\u0682"+
    "\0\u06b3\0\u06e4\0\u0715\0\u0746\0\u0777\0\u07a8\0\u07d9\0\u080a"+
    "\0\u083b\0\u086c\0\u089d\0\u08ce\0\u08ff\0\u0930\0\u0961\0\u0992"+
    "\0\61\0\u09c3\0\u09f4\0\u0a25\0\u0a56\0\u0a87\0\u0ab8\0\u0ae9"+
    "\0\u0b1a\0\u0b4b\0\u0b7c\0\u0bad\0\u0bde\0\u0c0f\0\u0c40\0\u0c71"+
    "\0\u0ca2\0\u0cd3\0\u0d04\0\u0d35\0\u0d66\0\u0d97\0\u0dc8\0\u0df9"+
    "\0\u0e2a\0\u0e5b\0\u0e8c\0\u0ebd\0\u0eee\0\u0f1f\0\u0f50\0\u0f81"+
    "\0\u0fb2\0\u0fe3\0\u1014\0\u1045\0\u1076\0\u10a7\0\u10d8\0\u1109"+
    "\0\u113a\0\u116b\0\u119c\0\u11cd\0\61\0\u11fe\0\u122f\0\u1260"+
    "\0\u1291\0\u12c2\0\u12f3\0\u1324\0\u1355\0\u1386\0\u13b7\0\u13e8"+
    "\0\u1419\0\u144a\0\u147b\0\u14ac\0\u14dd\0\u150e\0\u153f\0\u1570"+
    "\0\u15a1\0\61\0\u15d2\0\u1603\0\61\0\u1634\0\u1665\0\u1696"+
    "\0\u16c7\0\u16f8\0\u1729\0\u175a\0\u178b\0\u17bc\0\u17ed\0\u181e"+
    "\0\u184f\0\u1880\0\u18b1\0\u18e2\0\u1913\0\u1944\0\u1975\0\u19a6"+
    "\0\u19d7\0\u1a08\0\u1a39\0\u1a6a\0\61\0\u1a9b\0\u1acc\0\u1afd"+
    "\0\u1b2e\0\61\0\u1b5f\0\u1b90\0\u1bc1\0\u1bf2\0\u1c23\0\u1c54"+
    "\0\u1c85\0\u1cb6\0\u1ce7\0\u1d18\0\u1d49\0\u1d7a\0\u1dab\0\61"+
    "\0\u1ddc\0\u1e0d\0\u1e3e\0\u1e6f\0\u1ea0\0\u1ed1\0\61\0\u1f02"+
    "\0\u1f33\0\u1f64\0\61\0\u1f95\0\61\0\u1fc6\0\u1ff7\0\u2028"+
    "\0\61\0\u2059\0\u208a\0\u20bb\0\u20ec\0\u211d\0\u214e\0\u217f"+
    "\0\u21b0\0\u21e1\0\u2212\0\u2243\0\u2274\0\u22a5\0\u22d6\0\u2307"+
    "\0\u2338\0\u2369\0\u239a\0\u23cb\0\u23fc\0\u242d\0\u245e\0\u248f"+
    "\0\u24c0\0\u24f1\0\61\0\u2522\0\61\0\u2553\0\u2584\0\u25b5"+
    "\0\u25e6\0\u2617\0\u2648\0\u2679\0\u26aa\0\61\0\u26db\0\u270c"+
    "\0\u273d\0\u276e\0\61\0\u279f\0\u27d0\0\61\0\u2801\0\u2832"+
    "\0\u2863\0\u2894\0\u28c5\0\u28f6\0\u2927\0\u2958\0\61\0\u2989"+
    "\0\u29ba\0\u29eb\0\u2a1c\0\u2a4d\0\u2a7e\0\u2aaf\0\u2ae0\0\u2b11"+
    "\0\u2b42\0\u2b73\0\u2ba4\0\u2bd5\0\u2c06\0\u2c37\0\u2c68\0\61"+
    "\0\u2c99\0\u2cca\0\u2cfb\0\u2d2c\0\u2d5d\0\u2d8e\0\u2dbf\0\61"+
    "\0\61\0\u2df0\0\u2e21\0\u2e52\0\u2e83\0\u2eb4\0\u2ee5\0\u2f16"+
    "\0\61\0\u2f47\0\u2f78\0\u2fa9\0\u2fda\0\u300b\0\u303c\0\u306d"+
    "\0\61\0\u309e\0\61\0\u30cf\0\u3100\0\u3131\0\u3162\0\61"+
    "\0\u3193\0\u31c4\0\61\0\u31f5\0\u3226\0\u3257\0\u3288\0\u32b9"+
    "\0\u32ea\0\u331b\0\u334c\0\u337d\0\u33ae\0\u33df\0\u3410\0\u3441"+
    "\0\u3472\0\u34a3\0\u34d4\0\u3505\0\u3536\0\u3567\0\u3598\0\61"+
    "\0\u35c9\0\u35fa\0\u362b\0\u365c\0\61\0\61\0\61\0\u368d"+
    "\0\u36be\0\u36ef\0\u3720\0\u3751\0\u3782\0\61\0\u37b3\0\61"+
    "\0\61\0\u37e4\0\u3815\0\61\0\61\0\61\0\u3846\0\u3877"+
    "\0\61\0\u38a8\0\u38d9\0\u390a\0\61\0\61\0\u393b\0\61";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[344];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\2"+
    "\1\4\1\11\11\2\1\12\4\2\1\13\2\2\1\14"+
    "\3\2\1\15\1\2\1\16\1\2\1\17\1\20\1\2"+
    "\1\21\1\22\1\23\1\2\1\24\1\2\1\25\1\2"+
    "\1\26\1\27\1\30\61\0\2\3\1\0\56\3\2\0"+
    "\1\4\5\0\1\4\53\0\1\5\1\0\1\7\56\0"+
    "\1\5\1\31\1\7\56\0\1\7\55\0\6\32\1\33"+
    "\1\34\51\32\12\0\1\35\1\0\1\36\1\37\1\0"+
    "\1\40\54\0\1\41\1\0\1\42\63\0\1\43\1\44"+
    "\51\0\1\45\3\0\1\46\1\47\12\0\1\50\52\0"+
    "\1\51\52\0\1\52\1\0\1\53\61\0\1\54\63\0"+
    "\1\55\13\0\1\56\33\0\1\57\2\0\1\60\60\0"+
    "\1\61\77\0\1\62\44\0\1\63\52\0\1\64\4\0"+
    "\1\65\60\0\1\66\40\0\61\32\12\0\1\67\4\0"+
    "\1\70\55\0\1\71\57\0\1\72\64\0\1\73\114\0"+
    "\1\74\23\0\1\75\62\0\1\76\71\0\1\77\45\0"+
    "\1\100\71\0\1\101\47\0\1\102\60\0\1\103\102\0"+
    "\1\104\47\0\1\105\51\0\1\106\3\0\1\107\50\0"+
    "\1\110\56\0\1\111\4\0\1\112\54\0\1\113\105\0"+
    "\1\114\37\0\1\115\25\0\1\116\42\0\1\117\102\0"+
    "\1\120\62\0\1\121\27\0\1\122\55\0\1\123\56\0"+
    "\1\124\56\0\1\125\66\0\1\126\55\0\1\127\57\0"+
    "\1\130\62\0\1\131\65\0\1\132\101\0\1\133\45\0"+
    "\1\134\42\0\1\135\121\0\1\136\20\0\1\137\120\0"+
    "\1\140\26\0\1\141\50\0\1\142\62\0\1\143\55\0"+
    "\1\144\75\0\1\145\45\0\1\146\57\0\1\147\71\0"+
    "\1\150\55\0\1\151\52\0\1\152\60\0\1\153\60\0"+
    "\1\154\60\0\1\155\60\0\1\156\60\0\1\157\63\0"+
    "\1\160\56\0\1\161\60\0\1\162\66\0\1\163\66\0"+
    "\1\164\51\0\1\165\73\0\1\166\51\0\1\167\44\0"+
    "\1\170\66\0\1\171\54\0\1\172\61\0\1\173\63\0"+
    "\1\174\54\0\1\175\66\0\1\176\54\0\1\177\103\0"+
    "\1\200\36\0\1\201\76\0\1\202\55\0\1\203\46\0"+
    "\1\204\75\0\1\205\41\0\1\206\112\0\1\207\51\0"+
    "\1\210\32\0\1\211\62\0\1\212\61\0\1\213\62\0"+
    "\1\214\101\0\1\215\33\0\1\216\60\0\1\217\66\0"+
    "\1\220\74\0\1\221\40\0\1\222\57\0\1\223\11\0"+
    "\1\224\51\0\1\225\74\0\1\226\40\0\1\227\70\0"+
    "\1\230\64\0\1\231\47\0\1\232\63\0\1\233\11\0"+
    "\1\234\44\0\1\235\101\0\1\236\35\0\1\237\64\0"+
    "\1\240\66\0\1\241\52\0\1\242\55\0\1\243\110\0"+
    "\1\244\31\0\1\245\104\0\1\246\42\0\1\247\46\0"+
    "\1\250\103\0\1\251\1\252\36\0\1\253\66\0\1\254"+
    "\66\0\1\255\63\0\1\256\37\0\1\257\63\0\1\260"+
    "\62\0\1\261\60\0\1\262\55\0\1\263\64\0\1\264"+
    "\52\0\1\265\71\0\1\266\76\0\1\267\41\0\1\270"+
    "\50\0\1\271\63\0\1\272\61\0\1\273\105\0\1\274"+
    "\32\0\1\275\71\0\1\276\47\0\1\277\100\0\1\300"+
    "\1\301\4\0\1\302\35\0\1\303\52\0\1\304\60\0"+
    "\1\305\71\0\1\306\52\0\1\307\57\0\1\310\75\0"+
    "\1\311\45\0\1\312\60\0\1\313\56\0\1\314\77\0"+
    "\1\315\53\0\1\316\47\0\1\317\67\0\1\320\50\0"+
    "\1\321\62\0\1\322\103\0\1\323\42\0\1\324\50\0"+
    "\1\325\57\0\1\326\64\0\1\327\54\0\1\330\71\0"+
    "\1\331\60\0\1\332\50\0\1\333\114\0\1\334\41\0"+
    "\1\335\67\0\1\336\37\0\1\337\57\0\1\340\61\0"+
    "\1\341\54\0\1\342\60\0\1\343\74\0\1\344\46\0"+
    "\1\345\56\0\1\346\64\0\1\347\64\0\1\350\53\0"+
    "\1\351\62\0\1\352\57\0\1\353\101\0\1\354\33\0"+
    "\1\355\76\0\1\356\67\0\1\357\45\0\1\360\52\0"+
    "\1\361\56\0\1\362\64\0\1\363\57\0\1\364\64\0"+
    "\1\365\67\0\1\366\63\0\1\367\1\370\44\0\1\371"+
    "\12\0\1\372\1\373\33\0\1\374\61\0\1\375\62\0"+
    "\1\376\67\0\1\377\51\0\1\u0100\56\0\1\u0101\113\0"+
    "\1\u0102\27\0\1\u0103\64\0\1\u0104\56\0\1\u0105\54\0"+
    "\1\u0106\66\0\1\u0107\63\0\1\u0108\51\0\1\u0109\70\0"+
    "\1\u010a\53\0\1\u010b\53\0\1\u010c\71\0\1\u010d\55\0"+
    "\1\u010e\52\0\1\u010f\71\0\1\u0110\47\0\1\u0111\100\0"+
    "\1\u0112\43\0\1\u0113\67\0\1\u0114\50\0\1\u0115\56\0"+
    "\1\u0116\2\0\1\u0117\63\0\1\u0118\63\0\1\u0119\52\0"+
    "\1\u011a\57\0\1\u011b\56\0\1\u011c\73\0\1\u011d\66\0"+
    "\1\u011e\55\0\1\u011f\67\0\1\u0120\47\0\1\u0121\62\0"+
    "\1\u0122\67\0\1\u0123\62\0\1\u0124\42\0\1\u0125\62\0"+
    "\1\u0126\60\0\1\u0127\72\0\1\u0128\37\0\1\u0129\57\0"+
    "\1\u012a\66\0\1\u012b\60\0\1\u012c\54\0\1\u012d\55\0"+
    "\1\u012e\64\0\1\u012f\52\0\1\u0130\66\0\1\u0131\64\0"+
    "\1\u0132\60\0\1\u0133\53\0\1\u0134\56\0\1\u0135\63\0"+
    "\1\u0136\52\0\1\u0137\61\0\1\u0138\56\0\1\u0139\65\0"+
    "\1\u013a\56\0\1\u013b\55\0\1\u013c\72\0\1\u013d\61\0"+
    "\1\u013e\57\0\1\u013f\53\0\1\u0140\67\0\1\u0141\47\0"+
    "\1\u0142\55\0\1\u0143\102\0\1\u0144\41\0\1\u0145\56\0"+
    "\1\u0146\63\0\1\u0147\71\0\1\u0148\44\0\1\u0149\60\0"+
    "\1\u014a\65\0\1\u014b\67\0\1\u014c\44\0\1\u014d\74\0"+
    "\1\u014e\52\0\1\u014f\56\0\1\u0150\70\0\1\u0151\46\0"+
    "\1\u0152\61\0\1\u0153\63\0\1\u0154\66\0\1\u0155\45\0"+
    "\1\u0156\64\0\1\u0157\64\0\1\u0158\34\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[14700];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String ZZ_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\1\0\1\11\24\1\2\11\2\0\1\11\35\0\1\11"+
    "\53\0\1\11\24\0\1\11\2\0\1\11\27\0\1\11"+
    "\4\0\1\11\15\0\1\11\6\0\1\11\3\0\1\11"+
    "\1\0\1\11\3\0\1\11\31\0\1\11\1\0\1\11"+
    "\10\0\1\11\4\0\1\11\1\1\1\0\1\11\10\0"+
    "\1\11\20\0\1\11\7\0\2\11\7\0\1\11\7\0"+
    "\1\11\1\0\1\11\4\0\1\11\2\0\1\11\24\0"+
    "\1\11\4\0\3\11\6\0\1\11\1\0\2\11\2\0"+
    "\3\11\2\0\1\11\3\0\2\11\1\0\1\11";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[344];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */
  private int yychar;

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn;

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;

  /* user code: */
    public Object getLVal () { return null; }
    public void yyerror (String msg) {
        Error.Error(msg);
    }


  /**
   * Creates a new scanner
   * There is also a java.io.InputStream version of this constructor.
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public Yylex(java.io.Reader in) {
    this.zzReader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  public Yylex(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 138) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead);

      /* translate stored positions */
      zzEndRead-= zzStartRead;
      zzCurrentPos-= zzStartRead;
      zzMarkedPos-= zzStartRead;
      zzStartRead = 0;
    }

    /* is the buffer big enough? */
    if (zzCurrentPos >= zzBuffer.length) {
      /* if not: blow it up */
      char newBuffer[] = new char[zzCurrentPos*2];
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
      zzBuffer = newBuffer;
    }

    /* finally: fill the buffer with new input */
    int numRead = zzReader.read(zzBuffer, zzEndRead,
                                            zzBuffer.length-zzEndRead);

    if (numRead > 0) {
      zzEndRead+= numRead;
      return false;
    }
    // unlikely but not impossible: read 0 characters, but not at end of stream    
    if (numRead == 0) {
      int c = zzReader.read();
      if (c == -1) {
        return true;
      } else {
        zzBuffer[zzEndRead++] = (char) c;
        return false;
      }     
    }

	// numRead < 0
    return true;
  }

    
  /**
   * Closes the input stream.
   */
  public final void yyclose() throws java.io.IOException {
    zzAtEOF = true;            /* indicate end of file */
    zzEndRead = zzStartRead;  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close();
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * @param reader   the new input stream 
   */
  public final void yyreset(java.io.Reader reader) {
    zzReader = reader;
    zzAtBOL  = true;
    zzAtEOF  = false;
    zzEOFDone = false;
    zzEndRead = zzStartRead = 0;
    zzCurrentPos = zzMarkedPos = 0;
    yyline = yychar = yycolumn = 0;
    zzLexicalState = YYINITIAL;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final String yytext() {
    return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
  }


  /**
   * Returns the character at position <tt>pos</tt> from the 
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer[zzStartRead+pos];
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) throws java.lang.Error {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new java.lang.Error(message);
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  throws java.lang.Error {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public int yylex() throws java.io.IOException, java.lang.Error {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    char [] zzBufferL = zzBuffer;
    char [] zzCMapL = ZZ_CMAP;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      yychar+= zzMarkedPosL-zzStartRead;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
  
      zzState = ZZ_LEXSTATE[zzLexicalState];


      zzForAction: {
        while (true) {
    
          if (zzCurrentPosL < zzEndReadL)
            zzInput = zzBufferL[zzCurrentPosL++];
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = zzBufferL[zzCurrentPosL++];
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          int zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
        case 41: 
          { return Parser.MEDIUMINTERFACE;
          }
        case 47: break;
        case 42: 
          { return Parser.MAKENAMEDMEDIUM;
          }
        case 48: break;
        case 9: 
          { return Parser.SCALE;
          }
        case 49: break;
        case 15: 
          { return Parser.SAMPLER;
          }
        case 50: break;
        case 6: 
          { return Parser.STRING;
          }
        case 51: break;
        case 44: 
          { return Parser.COORDSYSTRANSFORM;
          }
        case 52: break;
        case 27: 
          { return Parser.ACCELERATOR;
          }
        case 53: break;
        case 35: 
          { return Parser.TRANSFORMTIMES;
          }
        case 54: break;
        case 10: 
          { return Parser.SHAPE;
          }
        case 55: break;
        case 14: 
          { return Parser.TEXTURE;
          }
        case 56: break;
        case 28: 
          { return Parser.LIGHTSOURCE;
          }
        case 57: break;
        case 39: 
          { return Parser.AREALIGHTSOURCE;
          }
        case 58: break;
        case 40: 
          { return Parser.CONCATTRANSFORM;
          }
        case 59: break;
        case 33: 
          { return Parser.NAMEDMATERIAL;
          }
        case 60: break;
        case 31: 
          { return Parser.ATTRIBUTEEND;
          }
        case 61: break;
        case 38: 
          { return Parser.ACTIVETRANSFORM;
          }
        case 62: break;
        case 37: 
          { return Parser.OBJECTINSTANCE;
          }
        case 63: break;
        case 29: 
          { return Parser.OBJECTBEGIN;
          }
        case 64: break;
        case 43: 
          { return Parser.COORDINATESYSTEM;
          }
        case 65: break;
        case 19: 
          { return Parser.MATERIAL;
          }
        case 66: break;
        case 17: 
          { return Parser.INCLUDE;
          }
        case 67: break;
        case 16: 
          { return Parser.ENDTIME;
          }
        case 68: break;
        case 11: 
          { return Parser.LOOKAT;
          }
        case 69: break;
        case 34: 
          { return Parser.ATTRIBUTEBEGIN;
          }
        case 70: break;
        case 18: 
          { return Parser.IDENTITY;
          }
        case 71: break;
        case 12: 
          { return Parser.CAMERA;
          }
        case 72: break;
        case 8: 
          { return Parser.FILM;
          }
        case 73: break;
        case 4: 
          { return Parser.LBRACK;
          }
        case 74: break;
        case 5: 
          { return Parser.RBRACK;
          }
        case 75: break;
        case 25: 
          { return Parser.INTEGRATOR;
          }
        case 76: break;
        case 21: 
          { return Parser.TRANSLATE;
          }
        case 77: break;
        case 3: 
          { return Parser.NUMBER;
          }
        case 78: break;
        case 7: 
          { return Parser.ALL;
          }
        case 79: break;
        case 22: 
          { return Parser.TRANSFORM;
          }
        case 80: break;
        case 20: 
          { return Parser.WORLDEND;
          }
        case 81: break;
        case 1: 
          { Error.Error("Illegal character: %c (0x%x)", yytext().charAt(0), (int)yytext().charAt(0));
          }
        case 82: break;
        case 24: 
          { return Parser.OBJECTEND;
          }
        case 83: break;
        case 45: 
          { return Parser.MAKENAMEDMATERIAL;
          }
        case 84: break;
        case 23: 
          { return Parser.STARTTIME;
          }
        case 85: break;
        case 13: 
          { return Parser.ROTATE;
          }
        case 86: break;
        case 36: 
          { return Parser.TRANSFORMBEGIN;
          }
        case 87: break;
        case 30: 
          { return Parser.PIXELFILTER;
          }
        case 88: break;
        case 32: 
          { return Parser.TRANSFORMEND;
          }
        case 89: break;
        case 46: 
          { return Parser.REVERSEORIENTATION;
          }
        case 90: break;
        case 26: 
          { return Parser.WORLDBEGIN;
          }
        case 91: break;
        case 2: 
          { 
          }
        case 92: break;
        default: 
          if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
            zzAtEOF = true;
            return YYEOF;
          } 
          else {
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
