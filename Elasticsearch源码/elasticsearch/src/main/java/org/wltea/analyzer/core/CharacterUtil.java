package org.wltea.analyzer.core;

class CharacterUtil {
	
	public static final int CHAR_USELESS = 0;
	public static final int CHAR_ARABIC = 0X00000001;
	
	public static final int CHAR_ENGLISH = 0X00000002;
	
	public static final int CHAR_CHINESE = 0X00000004;
	
	public static final int CHAR_OTHER_CJK = 0X00000008;
	
	static int identifyCharType(char input){
		if(input >= '0' && input <= '9'){
			return CHAR_ARABIC;
			
		}else if((input >= 'a' && input <= 'z')
				|| (input >= 'A' && input <= 'Z')){
			return CHAR_ENGLISH;
			
		}else {
			Character.UnicodeBlock ub = Character.UnicodeBlock.of(input);
			if(ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub ==Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
					||ub ==Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
					||ub ==Character.UnicodeBlock.GENERAL_PUNCTUATION
					||ub ==Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
					||ub ==Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
					||ub ==Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
					||(input <= 47 && input >= 33) || (input <= 96 && input >= 91)
					||(input <= 126 && input >= 123) ||input == 'β' || input == 'α' ||input == 'γ'
					|| input == 'Ⅰ' || input == 'Ⅱ' || input == 'Ⅲ' || input == 'Ⅳ' || input == 'Ⅴ'
//					|| input == 'Ⅰ' || input == 'Ⅱ' || input == 'Ⅲ' || input == 'Ⅳ'|| input == 'Ⅴ'
					|| input == 'Ⅵ' || input == 'Ⅶ' || input == 'Ⅷ' || input == 'Ⅸ' || input == 'Ⅹ'
					){
				//鐩墠宸茬煡鐨勪腑鏂囧瓧绗TF-8闆嗗悎
				return CHAR_CHINESE;
				
			}else if(ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS //鍏ㄨ鏁板瓧瀛楃鍜屾棩闊╁瓧绗�
					//闊╂枃瀛楃闆�
					|| ub == Character.UnicodeBlock.HANGUL_SYLLABLES 
					|| ub == Character.UnicodeBlock.HANGUL_JAMO
					|| ub == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
					//鏃ユ枃瀛楃闆�
					|| ub == Character.UnicodeBlock.HIRAGANA //骞冲亣鍚�
					|| ub == Character.UnicodeBlock.KATAKANA //鐗囧亣鍚�
					|| ub == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS){
				return CHAR_OTHER_CJK;
				
			}
		}
		//鍏朵粬鐨勪笉鍋氬鐞嗙殑瀛楃
		return CHAR_USELESS;
	}
	
	/**
	 * 杩涜瀛楃瑙勬牸鍖栵紙鍏ㄨ杞崐瑙掞紝澶у啓杞皬鍐欏鐞嗭級
	 * @param input
	 * @return char
	 */
	static char regularize(char input,boolean lowercase){
        if (input == 12288) {
            input = (char) 32;
            
        }else if (input > 65280 && input < 65375) {
            input = (char) (input - 65248);
            
        }else if (input >= 'A' && input <= 'Z' &&lowercase) {
        	input += 32;
		}
        
        return input;
	}
	public static void main(String[] args) {
		System.out.println(identifyCharType('a'));
		System.out.println((char)32);
	}
}
