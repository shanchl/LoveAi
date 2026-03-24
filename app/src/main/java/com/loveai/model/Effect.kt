package com.loveai.model

/**
 * 效果类型枚举 - 基础类型
 */
enum class EffectType {
    HEART_RAIN,     // 心形粒子飘落
    FIREWORK,       // 烟花绽放
    STARRY_SKY,     // 星空流星
    PETAL_FALL,     // 花瓣飘散
    BUBBLE_FLOAT,   // 泡泡上浮
    TYPEWRITER,     // 打字机效果
    HEART_PULSE,    // 爱心脉冲
    RIPPLE,         // 水波纹扩散
    SNOW_FALL,      // 飘雪飘落
    METEOR_SHOWER,  // 流星雨
    BUTTERFLY,      // 蝴蝶飞舞
    AURORA          // 极光波浪
}

/**
 * 效果变体配置 - 每个变体有独特的视觉效果参数
 */
data class EffectVariant(
    val id: Int,                        // 变体ID (0-49)
    val baseType: EffectType,           // 基础类型
    val variantName: String,            // 变体名称
    val primaryColor: Int,              // 主色调 (Color Int)
    val secondaryColor: Int,            // 副色调
    val backgroundColor: Int,           // 背景色
    val speed: Float = 1.0f,            // 动画速度倍率
    val particleCount: Int = 50,        // 粒子数量
    val message: String,                // 爱意文字
    val subMessage: String = ""         // 副标题文字
)

/**
 * 爱意效果数据模型
 */
data class Effect(
    val id: String,
    val variant: EffectVariant,
    val isFavorite: Boolean = false,
    val pageAssetUri: String? = null,
    val pageAssetName: String? = null
) {
    val type: EffectType get() = variant.baseType
    val message: String get() = variant.message
    val subMessage: String get() = variant.subMessage
}

/**
 * 爱意文字库 - 扩展到更多温馨爱意语句
 */
object LoveMessages {

    // 心形飘落类文字
    val heartRainMessages = listOf(
        Pair("你的笑容是我最美的风景", "每一次看见你，心都会跳快一拍"),
        Pair("爱你，是我做过最正确的事", "从相遇的那一刻起，我就知道了"),
        Pair("你是我掌心里的温柔", "握住你的手，就握住了整个世界"),
        Pair("喜欢你，喜欢了好久好久", "你不知道，我有多在乎你"),
        Pair("你是我心里藏着的小秘密", "一想到你，嘴角就会不自觉地上扬"),
        Pair("遇见你，是我此生最大的幸运", "感谢命运让我们相遇"),
        Pair("你是我最美的意外", "也是我最想留住的风景"),
        Pair("有你的地方，就是家", "不管去哪里，只要你在身边"),
        Pair("你是我心中的唯一", "无人可以取代的位置"),
        Pair("想你，是我每天必做的事", "一天不落地想你"),
        Pair("你的温柔，如春风拂面", "让我沉醉其中不愿醒来"),
        Pair("你是我心中最柔软的存在", "我想用一生来守护"),
        Pair("因为有你在", "平淡的日子都闪闪发光"),
        Pair("你是我最珍贵的宝贝", "我会好好珍惜你"),
        Pair("你的出现，让我的世界变得完整", "缺了你，什么都不对")
    )

    // 烟花类文字
    val fireworkMessages = listOf(
        Pair("遇见你是我最大的幸运", "这辈子，我只想和你一起看烟火"),
        Pair("你让我的世界绚烂多彩", "就像这烟花，为你而绽放"),
        Pair("有你的每一天，都像节日一样", "因为你，平凡的日子都熠熠生辉"),
        Pair("我想和你一起庆祝每一个瞬间", "每一次绽放，都是为了你"),
        Pair("愿我们的故事比烟火更绚烂", "比烟火更持久，更温暖"),
        Pair("你是我生命中最美的烟火", "点亮了我的整个世界"),
        Pair("愿每一朵烟火", "都载着我对你的祝福"),
        Pair("陪你一起看烟火", "是我最大的心愿"),
        Pair("你是我眼中最璀璨的光芒", "比烟火更耀眼"),
        Pair("愿我们的爱如烟火般绚烂", "即使短暂也要燃烧到极致"),
        Pair("每一次烟火绽放", "都是我对你的思念"),
        Pair("你是我心中永远绽放的花火", "永不熄灭")
    )

    // 星空类文字
    val starrySkyMessages = listOf(
        Pair("愿我们的爱如星辰永恒", "亿万星辰中，我只看见你"),
        Pair("你是我仰望的那颗最亮的星", "每个夜晚，我都在和你说悄悄话"),
        Pair("有你，夜空都变得更加温柔", "我愿做你身旁最近的那颗星"),
        Pair("星河很美，但不如你的眼睛", "因为那里有整个宇宙"),
        Pair("我想陪你看遍所有的星空", "直到看见流星，许下和你永远的愿望"),
        Pair("漫天星辰，不及你眼中的光芒", "你是我最美的星光"),
        Pair("在这浩瀚星河中", "我只为你心跳"),
        Pair("你是我心中的北极星", "指引我前进的方向"),
        Pair("愿陪你数尽漫天繁星", "每一颗都是我对你的思念"),
        Pair("你是我心中最亮的星", "照亮我前行的路"),
        Pair("星空再美，也不及你的万分之一", "你是最美的存在"),
        Pair("每颗星星都是一个愿望", "而我的愿望是你")
    )

    // 花瓣类文字
    val petalFallMessages = listOf(
        Pair("每一片花瓣都诉说着我对你的爱", "花开花落，我心里始终有你"),
        Pair("你像春天里最美的那朵花", "让我的世界充满了芬芳"),
        Pair("花落了还会再开，我对你的爱永不凋零", "四季更替，爱你如一"),
        Pair("愿你如花般绽放，美丽而自在", "我愿守护你一生的绽放"),
        Pair("花香弥漫，都是你的气息", "我沉醉其中，无法自拔"),
        Pair("你是我心中最美的花", "值得用一生来浇灌"),
        Pair("花开花落，四季更迭", "唯有对你的爱永恒不变"),
        Pair("你是春天里的暖风", "吹进了我冰冷的心"),
        Pair("愿和你一起看遍花开花落", "走过每一个春夏秋冬"),
        Pair("你是我生命中最美的风景", "胜过万千花海"),
        Pair("你如花般美丽动人", "让我为你着迷"),
        Pair("每一朵花都在诉说", "我对你的爱意")
    )

    // 泡泡类文字
    val bubbleMessages = listOf(
        Pair("你是我心中最美的泡泡", "轻盈透明，却能折射整个世界"),
        Pair("和你在一起，感觉像漂浮在云端", "轻盈又甜蜜，美得让人心疼"),
        Pair("每一个泡泡里，都装着我对你的思念", "五彩斑斓，像极了爱情"),
        Pair("爱你就像吹泡泡", "每一个都是小小的幸福"),
        Pair("你让我的生活充满了色彩和光泽", "就像阳光下的泡泡，璀璨夺目"),
        Pair("愿我们的爱情像泡泡一样", "五彩缤纷，闪闪发光"),
        Pair("每一个泡泡都是一个梦", "梦里全是你"),
        Pair("你是我心中最纯净的存在", "像透明的泡泡，美好而珍贵"),
        Pair("和你的每一天都像做梦", "愿永远不要醒来"),
        Pair("你让我的世界变得轻盈", "像是漂浮在幸福之中"),
        Pair("你是我心中最美的颜色", "点亮了我的生活"),
        Pair("愿为你守护这份纯真", "像泡泡一样透明美好")
    )

    // 打字机类文字
    val typewriterMessages = listOf(
        Pair("我爱你，从现在到永远", "这句话，我要说给你听一辈子"),
        Pair("遇见你之前，我不知道幸福是什么", "遇见你之后，我知道了，是你"),
        Pair("有些人，一旦遇见，便是一生", "你就是那个让我确定终身的人"),
        Pair("我想把我所有的好，都给你", "把我的笑，把我的爱，全部都给你"),
        Pair("谢谢你，来到我的生命里", "让所有的平淡都有了意义"),
        Pair("我想牵着你的手，一直走下去", "走到世界的尽头"),
        Pair("你是我今生最美的遇见", "感谢命运让我遇见你"),
        Pair("每一天都想对你说", "我爱你，比昨天更多一点"),
        Pair("你是我写过最美的情书", "每一个字都是真心的"),
        Pair("想对你说的话太多", "但我最想说的是我爱你"),
        Pair("你是我心中最温柔的诗", "读一遍就感动一次"),
        Pair("有些话藏在心里很久了", "我爱你，今生今世"),
        Pair("我的世界里，只有你", "是我唯一的眷恋"),
        Pair("你是我所有美好的集合", "是我心中唯一的选择"),
        Pair("想陪你写完一生", "每一页都是关于你")
    )

    // 爱心脉冲类文字
    val heartPulseMessages = listOf(
        Pair("你是我的心跳，是我的生命", "没有你，我的世界将失去颜色"),
        Pair("我的心，只为你跳动", "每一下，都是对你无声的表白"),
        Pair("你住在我心里，赶也赶不走", "而我，心甘情愿"),
        Pair("爱你，是我心底最深的执念", "无法割舍，无法忘记"),
        Pair("你是我心脏最柔软的地方", "触碰到你，我就融化了"),
        Pair("我的心为你而跳动", "每一刻都在呼唤你的名字"),
        Pair("你的名字刻在我心上", "永远也抹不去"),
        Pair("你是我的专属心跳", "只为你加速"),
        Pair("我的心早已被你填满", "再也装不下别人"),
        Pair("每一次心跳都在说爱你", "你听见了吗"),
        Pair("你是我心中最深的印记", "刻骨铭心"),
        Pair("我的心已经给了你", "请好好珍惜")
    )

    // 水波纹类文字
    val rippleMessages = listOf(
        Pair("爱如涟漪，在我心中扩散", "每一圈，都比上一圈更深"),
        Pair("你在我心里激起的涟漪", "已经蔓延到了灵魂深处"),
        Pair("一遇见你，心便荡漾了", "再也平静不下来"),
        Pair("爱情如水，因你而起涟漪", "波波相连，永不停息"),
        Pair("你轻轻一笑，我心里就泛起层层涟漪", "多希望这涟漪永远不要平息"),
        Pair("你是那颗落入我心的石子", "激起无尽涟漪"),
        Pair("我的心因你而波动", "再也平静不下来"),
        Pair("你的出现，在我心中荡漾", "一圈又一圈，永不停歇"),
        Pair("爱情如水，温柔绵长", "流淌在我的心间"),
        Pair("你是我心中最美的涟漪", "荡漾着爱的气息"),
        Pair("每一次心动", "都是因为你而起的涟漪"),
        Pair("你是我心中永不平静的理由", "因为爱你，心一直荡漾")
    )

    // 飘雪类文字
    val snowFallMessages = listOf(
        Pair("每一片雪花，都是我对你的思念", "飘落在你的世界里，悄悄守候"),
        Pair("愿用满天飞雪，换你一个微笑", "这冬天因你而温暖"),
        Pair("雪花飘落的声音，是我轻唤你的名字", "一遍又一遍，从未停歇"),
        Pair("你是我心中最温柔的冬日暖阳", "让漫天白雪都失了颜色"),
        Pair("我想和你一起，踩着雪地的足迹", "走向漫长而温柔的未来"),
        Pair("每片雪花都是一个秘密", "秘密的名字，叫做你"),
        Pair("冬天再冷，因为有你在身边", "便觉得整个世界都暖了"),
        Pair("愿陪你看遍世间的雪", "也愿为你融化每一片"),
        Pair("你是我心中不融的那片雪", "洁白而珍贵"),
        Pair("飞雪连天，思你如昔", "一刻也不曾忘怀"),
        Pair("雪落无声，我的爱意有声", "轻轻诉说着你的名"),
        Pair("这漫天飞雪，送去我最深的爱", "愿温暖你整个冬天")
    )

    // 流星雨类文字
    val meteorShowerMessages = listOf(
        Pair("划过夜空的流星，是我对你的祝愿", "愿每一颗都能成真"),
        Pair("你是我今生最美的流星", "短暂而耀眼，令我永生难忘"),
        Pair("我许下的愿望，每一个都是你", "流星见证，永不改变"),
        Pair("流星划过的瞬间，我想到的是你", "就在那一刹那，爱意涌满心头"),
        Pair("愿我们的故事，比流星更久长", "比星光更璀璨"),
        Pair("每颗流星都是一封情书", "写满了我对你的爱"),
        Pair("你出现在我生命里，像一场流星雨", "美得让我来不及许愿"),
        Pair("我用流星来许愿，愿守护你一生", "今生今世，不离不弃"),
        Pair("漫天流星，许的都是你", "只要你好，什么都好"),
        Pair("流星雨中，我只看见你", "你是我最亮的那颗"),
        Pair("如果许愿真的有用", "我要把所有流星都许给你"),
        Pair("你是我心中最亮的流星", "照亮了我前行的夜路")
    )

    // 蝴蝶飞舞类文字
    val butterflyMessages = listOf(
        Pair("你如蝴蝶一般，翩翩飞进我心里", "就此扎根，再也飞不走"),
        Pair("爱你，就像追逐一只蝴蝶", "轻轻的，怕惊扰了你的美"),
        Pair("你是我心中那只最美的蝴蝶", "每次见你，心都会跟着飞翔"),
        Pair("蝴蝶飞过的地方，都是你留下的香气", "让我沉醉，无法自拔"),
        Pair("我愿是那只陪在你身边的蝴蝶", "伴你飞越每一片花海"),
        Pair("你的笑，像蝴蝶振翅的声音", "轻柔又动听"),
        Pair("和你在一起，是我最轻盈的时光", "像蝴蝶一样自由又快乐"),
        Pair("你是春天里最美的那只蝴蝶", "飞进了我的梦里，再也飞不走"),
        Pair("我愿化作蝴蝶，只为飞向你", "哪怕只有一刻，也心满意足"),
        Pair("每一次相遇，都像蝴蝶破茧", "美丽而珍贵，值得珍藏"),
        Pair("你翩然出现在我生命里", "从此，我的世界春意盎然"),
        Pair("爱你，如蝴蝶恋花", "深情而执着，不离不弃")
    )

    // 极光类文字
    val auroraMessages = listOf(
        Pair("你是我生命里的极光", "绚烂如斯，令我心醉神迷"),
        Pair("极光只在最纯净的地方绽放", "就像我对你的爱，纯粹而美好"),
        Pair("愿你我之间，有极光般的浪漫", "跨越千山万水，亦不褪色"),
        Pair("你的眼睛里有极光的颜色", "绿的蓝的紫的，全是梦幻"),
        Pair("此生若能与你一同看极光", "便是世间最浪漫的事"),
        Pair("你是我心中最绚烂的极光", "出现在最暗的夜，带来最美的光"),
        Pair("爱情就像极光，可遇而不可求", "幸运的是，我遇见了你"),
        Pair("愿为你点亮整片夜空的极光", "只愿映照你的笑颜"),
        Pair("你的存在，让我相信童话", "就像极光真实存在于夜空"),
        Pair("有你在身边，寒夜也变得温柔", "如极光般绮丽梦幻"),
        Pair("你是那道划破黑暗的极光", "让我的世界从此充满希望"),
        Pair("愿我们的爱，如极光般经久不衰", "亿万年后依然璀璨")
    )

    /**
     * 根据效果类型随机获取一条爱意文字
     */
    fun getRandomMessage(type: EffectType): Pair<String, String> {
        return when (type) {
            EffectType.HEART_RAIN -> heartRainMessages.random()
            EffectType.FIREWORK -> fireworkMessages.random()
            EffectType.STARRY_SKY -> starrySkyMessages.random()
            EffectType.PETAL_FALL -> petalFallMessages.random()
            EffectType.BUBBLE_FLOAT -> bubbleMessages.random()
            EffectType.TYPEWRITER -> typewriterMessages.random()
            EffectType.HEART_PULSE -> heartPulseMessages.random()
            EffectType.RIPPLE -> rippleMessages.random()
            EffectType.SNOW_FALL -> snowFallMessages.random()
            EffectType.METEOR_SHOWER -> meteorShowerMessages.random()
            EffectType.BUTTERFLY -> butterflyMessages.random()
            EffectType.AURORA -> auroraMessages.random()
        }
    }
}
