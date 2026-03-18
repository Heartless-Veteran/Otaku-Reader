/**
 * SFX (Sound Effects) Translator Service
 * 
 * Intelligently identifies and translates sound effects in web novels
 * while preserving emotional impact and context.
 */

export type SFXStyle = 'literal' | 'cultural' | 'mixed';
export type NovelGenre = 'wuxia' | 'modern' | 'fantasy' | 'romance' | 'horror' | 'scifi';

export interface NovelContext {
  genre?: NovelGenre;
  sourceLanguage: 'zh' | 'en' | 'ja' | 'ko';
  targetLanguage: 'zh' | 'en' | 'ja' | 'ko';
  characterContext?: {
    personality?: 'serious' | 'playful' | 'shy' | 'aggressive' | 'elegant';
    age?: 'child' | 'teen' | 'adult' | 'elder';
  };
}

export interface SFXMatch {
  original: string;
  translation: string;
  startIndex: number;
  endIndex: number;
  category: SFXCategory;
  confidence: number;
}

export type SFXCategory = 
  | 'laughter' 
  | 'action' 
  | 'emotional' 
  | 'impact' 
  | 'movement' 
  | 'animal' 
  | 'weather' 
  | 'mechanical'
  | 'magical';

export interface SFXEntry {
  translations: Record<string, string[]>;
  category: SFXCategory;
  context?: string;
}

/**
 * Comprehensive SFX dictionary with cultural adaptations
 */
export const sfxDictionary: Record<string, SFXEntry> = {
  // === CHINESE SFX ===
  // Laughter
  '哈哈哈': {
    translations: {
      en: ['hahaha', 'ha ha ha', '*laughs heartily*'],
      ja: ['ハハハ', 'あはは'],
      ko: ['하하하', '크크크']
    },
    category: 'laughter'
  },
  '呵呵': {
    translations: {
      en: ['hehe', '*chuckles*', 'heh'],
      ja: ['ふふ', 'ふふふ'],
      ko: ['헤헤', '훗']
    },
    category: 'laughter'
  },
  '嘿嘿': {
    translations: {
      en: ['heh heh', '*snickers*', 'hee hee'],
      ja: ['ヒヒ', 'ニヤニヤ'],
      ko: ['히히', '킥킥']
    },
    category: 'laughter',
    context: 'mischievous'
  },
  '咯咯': {
    translations: {
      en: ['giggle', '*giggles*', 'tee hee'],
      ja: ['クスクス'],
      ko: ['낄낄']
    },
    category: 'laughter',
    context: 'feminine'
  },
  
  // Action sounds
  '咔嚓': {
    translations: {
      en: ['*snap*', '*crack*', '*click*'],
      ja: ['カチッ'],
      ko: ['딸깍']
    },
    category: 'action',
    context: 'breaking/snapping sound'
  },
  '砰': {
    translations: {
      en: ['*bang*', '*boom*', '*thud*'],
      ja: ['バン', 'ドン'],
      ko: ['쾅', '쿵']
    },
    category: 'impact'
  },
  '啪': {
    translations: {
      en: ['*slap*', '*clap*', '*pop*'],
      ja: ['パン', 'パチッ'],
      ko: ['퍽', '짝']
    },
    category: 'impact'
  },
  '嗖': {
    translations: {
      en: ['*swoosh*', '*whoosh*', '*swish*'],
      ja: ['ビュッ', 'ヒュッ'],
      ko: ['슈웅']
    },
    category: 'movement'
  },
  '咻': {
    translations: {
      en: ['*zip*', '*zoom*', '*swish*'],
      ja: ['シュッ'],
      ko: ['쉭']
    },
    category: 'movement'
  },
  '咚': {
    translations: {
      en: ['*thump*', '*dong*', '*boom*'],
      ja: ['ドン'],
      ko: ['쿵']
    },
    category: 'impact'
  },
  '哐': {
    translations: {
      en: ['*clang*', '*clank*', '*crash*'],
      ja: ['ガン', 'ゴン'],
      ko: ['쾅']
    },
    category: 'impact',
    context: 'metallic'
  },
  
  // Emotional exclamations
  '啊': {
    translations: {
      en: ['ah', 'oh', 'huh'],
      ja: ['あ', 'あっ'],
      ko: ['아', '어']
    },
    category: 'emotional'
  },
  '哎呀': {
    translations: {
      en: ['oh my', 'oops', 'uh oh'],
      ja: ['あら', 'おや'],
      ko: ['어머', '아이구']
    },
    category: 'emotional'
  },
  '哇': {
    translations: {
      en: ['wow', 'whoa', 'wah'],
      ja: ['わっ', 'わあ'],
      ko: ['와', '와아']
    },
    category: 'emotional',
    context: 'surprise'
  },
  '呜': {
    translations: {
      en: ['wu', 'boohoo', '*sob*'],
      ja: ['うう', 'うっ'],
      ko: ['우우', '훌쩍']
    },
    category: 'emotional',
    context: 'sadness'
  },
  '哼': {
    translations: {
      en: ['hmph', 'humph', 'hmpf'],
      ja: ['ふん', 'ハッ'],
      ko: ['흥', '흠']
    },
    category: 'emotional',
    context: 'annoyance'
  },
  '哦': {
    translations: {
      en: ['oh', 'ooh', 'ah'],
      ja: ['おお', 'ああ'],
      ko: ['오', '오오']
    },
    category: 'emotional',
    context: 'realization'
  },
  
  // Wuxia/Xianxia specific
  '嗡': {
    translations: {
      en: ['*hum*', '*thrumm*', '*buzz*'],
      ja: ['ブーン'],
      ko: ['웅']
    },
    category: 'magical',
    context: 'energy/aura'
  },
  '轰': {
    translations: {
      en: ['*rumble*', '*roar*', '*crash*'],
      ja: ['ゴォォ', 'ドォォ'],
      ko: ['쿠르릉']
    },
    category: 'magical',
    context: 'explosion/thunder'
  },
  
  // === ENGLISH SFX ===
  'hahaha': {
    translations: {
      zh: ['哈哈哈', '哈哈', '呵呵'],
      ja: ['ハハハ'],
      ko: ['하하하']
    },
    category: 'laughter'
  },
  'hehehe': {
    translations: {
      zh: ['嘿嘿嘿', '呵呵呵'],
      ja: ['ヘヘヘ'],
      ko: ['헤헤헤']
    },
    category: 'laughter',
    context: 'mischievous'
  },
  '*snap*': {
    translations: {
      zh: ['咔嚓', '啪'],
      ja: ['カチッ'],
      ko: ['딸깍']
    },
    category: 'action'
  },
  '*click*': {
    translations: {
      zh: ['咔哒', '滴答'],
      ja: ['カチッ'],
      ko: ['딸깍']
    },
    category: 'action'
  },
  '*swoosh*': {
    translations: {
      zh: ['嗖', '咻'],
      ja: ['ビュッ'],
      ko: ['슈웅']
    },
    category: 'movement'
  },
  '*boom*': {
    translations: {
      zh: ['轰', '砰', '嘭'],
      ja: ['ドン', 'バン'],
      ko: ['쿵', '쾅']
    },
    category: 'impact'
  },
  '*bang*': {
    translations: {
      zh: ['砰', '嘭'],
      ja: ['バン'],
      ko: ['쾅']
    },
    category: 'impact'
  },
  '*clang*': {
    translations: {
      zh: ['哐', '锵'],
      ja: ['ガン', 'カン'],
      ko: ['쿵']
    },
    category: 'impact',
    context: 'metallic'
  },
  '*sigh*': {
    translations: {
      zh: ['叹气', '唉'],
      ja: ['ため息'],
      ko: ['한숨']
    },
    category: 'emotional'
  },
  '*gasp*': {
    translations: {
      zh: ['倒吸一口气', '啊'],
      ja: ['ハッ'],
      ko: ['헉']
    },
    category: 'emotional',
    context: 'surprise'
  },
  '*chuckle*': {
    translations: {
      zh: ['轻笑', '呵呵'],
      ja: ['クスッ'],
      ko: ['킬킬']
    },
    category: 'laughter'
  }
};

/**
 * Pattern definitions for detecting SFX in text
 */
const sfxPatterns = {
  // Laughter patterns (Chinese)
  chineseLaughter: /[哈呵嘿咯咯]{2,}/g,
  
  // Laughter patterns (English)
  englishLaughter: /\b(ha+\b|he+\b|ho+\b|tee\s*hee|giggle|snicker|chuckle)s?\b/gi,
  
  // Action sounds (Chinese onomatopoeia)
  chineseAction: /[咔嚓砰啪嗖咻咚哐嗡轰呜]{1,2}/g,
  
  // Emotional exclamations (Chinese)
  chineseEmotional: /[啊哎呀哇哦哼嘿哟嗯]{1,2}/g,
  
  // English SFX in asterisks (e.g., *click*, *boom*)
  asteriskSFX: /\*([a-zA-Z\s]+?)\*/g,
  
  // English onomatopoeia
  englishOnomatopoeia: /\b(bang|boom|snap|click|swoosh|whoosh|pop|crash|clang|thud|slam|ding|ring|beep)\b/gi,
  
  // Repeated characters (potential SFX)
  repeatedChars: /(.)(\1{2,})/g
};

/**
 * Genre-aware styling for SFX
 */
function applyGenreStyle(
  translation: string,
  category: SFXCategory,
  genre?: NovelGenre,
  style: SFXStyle = 'cultural'
): string {
  if (style === 'literal') {
    return translation;
  }
  
  if (style === 'mixed' && !translation.startsWith('*')) {
    return `*${translation}*`;
  }
  
  // Genre-specific styling
  switch (genre) {
    case 'wuxia':
    case 'fantasy':
      // Traditional/more dramatic styling
      if (category === 'impact' || category === 'magical') {
        return translation.toUpperCase();
      }
      break;
      
    case 'modern':
      // Casual, natural styling
      if (category === 'laughter') {
        return translation.toLowerCase();
      }
      break;
      
    case 'romance':
      // Softer styling
      if (category === 'emotional') {
        return translation;
      }
      break;
      
    case 'horror':
      // Sharper, more impactful
      if (category === 'impact' || category === 'emotional') {
        return translation.toUpperCase();
      }
      break;
  }
  
  return translation;
}

/**
 * Select appropriate translation based on context
 */
function selectTranslation(
  entry: SFXEntry,
  context: NovelContext,
  style: SFXStyle
): string {
  const translations = entry.translations[context.targetLanguage] || entry.translations['en'] || [];
  
  if (translations.length === 0) {
    return '';
  }
  
  // Select based on character context
  if (context.characterContext) {
    const { personality, age } = context.characterContext;
    
    // Laughter variations based on personality
    if (entry.category === 'laughter') {
      if (personality === 'playful' || age === 'child') {
        return translations.find(t => t.includes('ha')) || translations[0];
      }
      if (personality === 'elegant') {
        return translations.find(t => t.includes('chuckle') || t.includes('hehe')) || translations[0];
      }
      if (personality === 'serious') {
        return translations.find(t => t.startsWith('*')) || translations[0];
      }
    }
  }
  
  // Return first translation as default
  return translations[0];
}

/**
 * Detect SFX patterns in text and return matches
 */
export function detectSFX(text: string, sourceLanguage: string): SFXMatch[] {
  const matches: SFXMatch[] = [];
  
  // Check dictionary first (exact matches)
  for (const [pattern, entry] of Object.entries(sfxDictionary)) {
    const regex = new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g');
    let match;
    while ((match = regex.exec(text)) !== null) {
      matches.push({
        original: match[0],
        translation: '', // Will be filled later
        startIndex: match.index,
        endIndex: match.index + match[0].length,
        category: entry.category,
        confidence: 0.95
      });
    }
  }
  
  // Pattern-based detection for Chinese
  if (sourceLanguage === 'zh' || sourceLanguage === 'ja' || sourceLanguage === 'ko') {
    let match;
    while ((match = sfxPatterns.chineseLaughter.exec(text)) !== null) {
      if (!matches.some(m => m.startIndex === match.index)) {
        matches.push({
          original: match[0],
          translation: '',
          startIndex: match.index,
          endIndex: match.index + match[0].length,
          category: 'laughter',
          confidence: 0.85
        });
      }
    }
    
    while ((match = sfxPatterns.chineseAction.exec(text)) !== null) {
      if (!matches.some(m => m.startIndex === match!.index)) {
        matches.push({
          original: match[0],
          translation: '',
          startIndex: match.index,
          endIndex: match.index + match[0].length,
          category: 'action',
          confidence: 0.75
        });
      }
    }
  }
  
  // Pattern-based detection for English
  if (sourceLanguage === 'en') {
    let match;
    while ((match = sfxPatterns.asteriskSFX.exec(text)) !== null) {
      if (!matches.some(m => m.startIndex === match.index)) {
        matches.push({
          original: match[0],
          translation: '',
          startIndex: match.index,
          endIndex: match.index + match[0].length,
          category: 'action',
          confidence: 0.9
        });
      }
    }
    
    while ((match = sfxPatterns.englishLaughter.exec(text)) !== null) {
      if (!matches.some(m => m.startIndex === match.index)) {
        matches.push({
          original: match[0],
          translation: '',
          startIndex: match.index,
          endIndex: match.index + match[0].length,
          category: 'laughter',
          confidence: 0.8
        });
      }
    }
  }
  
  // Sort by start index and remove overlapping matches
  matches.sort((a, b) => a.startIndex - b.startIndex);
  const filteredMatches: SFXMatch[] = [];
  
  for (const match of matches) {
    const overlaps = filteredMatches.some(m => 
      (match.startIndex >= m.startIndex && match.startIndex < m.endIndex) ||
      (match.endIndex > m.startIndex && match.endIndex <= m.endIndex)
    );
    if (!overlaps) {
      filteredMatches.push(match);
    }
  }
  
  return filteredMatches;
}

/**
 * Main SFX translation function
 * 
 * @param text The text containing SFX to translate
 * @param context The novel context for intelligent translation
 * @param style The translation style preference
 * @returns Object with translated text and SFX metadata
 */
export function translateSFX(
  text: string,
  context: NovelContext,
  style: SFXStyle = 'cultural'
): {
  translatedText: string;
  matches: SFXMatch[];
  hasSFX: boolean;
} {
  const matches = detectSFX(text, context.sourceLanguage);
  
  if (matches.length === 0) {
    return { translatedText: text, matches: [], hasSFX: false };
  }
  
  // Build translated text
  let result = text;
  let offset = 0;
  
  for (const match of matches) {
    const entry = sfxDictionary[match.original];
    let translation: string;
    
    if (entry) {
      translation = selectTranslation(entry, context, style);
    } else {
      // Fallback: use AI or pattern-based translation
      translation = fallbackTranslate(match.original, match.category, context);
    }
    
    // Apply genre styling
    translation = applyGenreStyle(translation, match.category, context.genre, style);
    
    // Update match with translation
    match.translation = translation;
    
    // Replace in text
    const adjustedStart = match.startIndex + offset;
    const adjustedEnd = match.endIndex + offset;
    
    // Format based on style
    let replacement: string;
    if (style === 'cultural') {
      replacement = translation;
    } else if (style === 'literal') {
      replacement = translation;
    } else {
      // mixed: show original with translation
      replacement = `${translation}`;
    }
    
    result = result.substring(0, adjustedStart) + replacement + result.substring(adjustedEnd);
    offset += replacement.length - (match.endIndex - match.startIndex);
  }
  
  return {
    translatedText: result,
    matches,
    hasSFX: true
  };
}

/**
 * Fallback translation for unknown SFX
 */
function fallbackTranslate(
  original: string,
  category: SFXCategory,
  context: NovelContext
): string {
  // Simple phonetic approximation or category-based translation
  const fallbacks: Record<SFXCategory, Record<string, string>> = {
    laughter: {
      zh: '哈哈',
      en: 'haha',
      ja: 'ハハ',
      ko: '하하'
    },
    action: {
      zh: '啪',
      en: '*pop*',
      ja: 'パ',
      ko: '퍽'
    },
    emotional: {
      zh: '啊',
      en: 'ah',
      ja: 'あ',
      ko: '아'
    },
    impact: {
      zh: '砰',
      en: '*bang*',
      ja: 'バン',
      ko: '쾅'
    },
    movement: {
      zh: '嗖',
      en: '*swoosh*',
      ja: 'ビュ',
      ko: '슈웅'
    },
    animal: {
      zh: '呜',
      en: '*growl*',
      ja: 'ウー',
      ko: '으르'
    },
    weather: {
      zh: '轰',
      en: '*rumble*',
      ja: 'ゴロ',
      ko: '우르르'
    },
    mechanical: {
      zh: '咔',
      en: '*clank*',
      ja: 'ガキ',
      ko: '콰당'
    },
    magical: {
      zh: '嗡',
      en: '*hum*',
      ja: 'ブン',
      ko: '웅'
    }
  };
  
  return fallbacks[category]?.[context.targetLanguage] || original;
}

/**
 * Async translation using AI for complex SFX
 * This would integrate with Gemini Nano for context-aware translation
 */
export async function translateSFXWithAI(
  text: string,
  context: NovelContext,
  geminiClient: { generateContent: (prompt: string) => Promise<string> },
  style: SFXStyle = 'cultural'
): Promise<{
  translatedText: string;
  matches: SFXMatch[];
  hasSFX: boolean;
}> {
  const matches = detectSFX(text, context.sourceLanguage);
  
  if (matches.length === 0) {
    return { translatedText: text, matches: [], hasSFX: false };
  }
  
  // For complex cases, use AI
  const prompt = buildSFXPrompt(text, matches, context, style);
  
  try {
    const aiResponse = await geminiClient.generateContent(prompt);
    const aiTranslations = parseAIResponse(aiResponse, matches);
    
    // Apply AI translations
    let result = text;
    let offset = 0;
    
    for (let i = 0; i < matches.length; i++) {
      const match = matches[i];
      const translation = aiTranslations[i] || match.translation || fallbackTranslate(match.original, match.category, context);
      
      match.translation = applyGenreStyle(translation, match.category, context.genre, style);
      
      const adjustedStart = match.startIndex + offset;
      const adjustedEnd = match.endIndex + offset;
      
      result = result.substring(0, adjustedStart) + match.translation + result.substring(adjustedEnd);
      offset += match.translation.length - (match.endIndex - match.startIndex);
    }
    
    return {
      translatedText: result,
      matches,
      hasSFX: true
    };
  } catch (error) {
    // Fallback to dictionary-based translation
    return translateSFX(text, context, style);
  }
}

/**
 * Build prompt for AI SFX translation
 */
function buildSFXPrompt(
  text: string,
  matches: SFXMatch[],
  context: NovelContext,
  style: SFXStyle
): string {
  const sfxList = matches.map(m => `"${m.original}" (${m.category})`).join(', ');
  
  return `Translate these sound effects from ${context.sourceLanguage} to ${context.targetLanguage}:
${sfxList}

Context: ${context.genre || 'general'} novel
Style: ${style} translation
${context.characterContext ? `Character: ${context.characterContext.personality} ${context.characterContext.age}` : ''}

Original text: "${text}"

Rules:
1. Preserve the emotional impact and intensity
2. Match the genre tone (${context.genre || 'general'})
3. Use natural-sounding SFX in the target language
4. For laughter, match the character's personality
5. Return only the translations in order, one per line`;
}

/**
 * Parse AI response into translation array
 */
function parseAIResponse(response: string, matches: SFXMatch[]): string[] {
  const lines = response.trim().split('\n').filter(line => line.trim());
  return lines.map(line => line.replace(/^\d+\.\s*/, '').trim());
}

/**
 * Create HTML representation with tooltips
 */
export function createSFXHTML(
  text: string,
  matches: SFXMatch[],
  showOriginal: boolean = true
): string {
  if (!showOriginal || matches.length === 0) {
    return text;
  }
  
  let result = text;
  let offset = 0;
  
  for (const match of matches) {
    const adjustedStart = match.startIndex + offset;
    const adjustedEnd = match.endIndex + offset;
    
    const replacement = `<span class="sfx-translation" data-original="${match.original}" title="Original: ${match.original}">${match.translation}</span>`;
    
    result = result.substring(0, adjustedStart) + replacement + result.substring(adjustedEnd);
    offset += replacement.length - (match.endIndex - match.startIndex);
  }
  
  return result;
}

/**
 * Check if text contains SFX
 */
export function hasSFX(text: string, sourceLanguage: string): boolean {
  return detectSFX(text, sourceLanguage).length > 0;
}

/**
 * Get SFX statistics for a text
 */
export function getSFXStats(text: string, sourceLanguage: string): {
  total: number;
  byCategory: Record<SFXCategory, number>;
} {
  const matches = detectSFX(text, sourceLanguage);
  const byCategory: Record<SFXCategory, number> = {
    laughter: 0,
    action: 0,
    emotional: 0,
    impact: 0,
    movement: 0,
    animal: 0,
    weather: 0,
    mechanical: 0,
    magical: 0
  };
  
  for (const match of matches) {
    byCategory[match.category]++;
  }
  
  return {
    total: matches.length,
    byCategory
  };
}

export default {
  translateSFX,
  translateSFXWithAI,
  detectSFX,
  createSFXHTML,
  hasSFX,
  getSFXStats,
  sfxDictionary
};