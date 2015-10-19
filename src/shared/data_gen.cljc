(ns kandan.data-gen
  #?(:clj (:require [clojure.string :as string]
                    [clojure.test.check :as tc]
                    [clojure.test.check.generators :as gen]
                    [clojure.test.check.properties :as prop :include-macros true]
                    [datomic.api :as d]
                    [dato.db.utils :as dsu])
          :cljs (:require [cljs.test.check :as tc]
                          [cljs.test.check.generators :as gen]
                          [cljs.test.check.properties :as prop :include-macros true]
                          [clojure.string :as string]
                          [datascript :as d]
                          [dato.db.utils :as dsu]))
  #?(:clj (:import [java.util Date]
                   [java.net URI URL])))

(defn date
  ([]
   #?(:clj (Date.)
      :cljs (js/Date.)))
  ([time]
   #?(:clj (Date. time)
           :cljs (js/Date. time))))

(def gen-inst
  (gen/fmap #(date %) gen/pos-int))

(defn now []
  (date))

(def gen-datomic-ids
  (gen/fmap #(do % (d/tempid :db.part/user)) (gen/return nil)))

(def gen-dato-guids
  (gen/fmap #(do % (d/squuid)) (gen/return nil)))

(defn make-long-string-gen [min]
  (gen/not-empty (gen/fmap (fn [[fxd vr]]
                             (clojure.string/join (into fxd vr)))
                           (gen/tuple (gen/vector gen/char-alphanumeric min)
                                      (gen/vector gen/char-alphanumeric)))))

(def japanese-surnames-and-romaji
  {"稲田" "Inada", "村松" "Muramatsu", "上村" "Jouon", "西本" "Nishimoto", "白井" "Hirai", "佐野" "Sano", "野口" "Noguchi", "岸" "Kishi", "川口" "Kawaguchi", "大久保" "Ookubo", "大野" "Oono", "梶原" "Kajihara", "木村" "Kimura", "岡" "Oka", "永井" "Nagai", "長野" "Nagano", "川村" "Kawamura", "三好" "Miyoshi", "落合" "Ochiai", "高瀬" "Takase", "宮城" "Miyagi", "遠藤" "Endou", "岩井" "Iwai", "窪田" "Kubota", "矢島" "Yajima", "小原" "Kohara", "川島" "Kawashima", "河野" "Kouno", "齋藤" "Saitou", "関" "Seki", "相沢" "Aizawa", "東" "Higashi", "岡野" "Okano", "大島" "Ooshima", "島田" "Shimada", "成田" "Narita", "新谷" "Niiya", "丹羽" "Niwa", "広瀬" "Hirose", "向井" "Mukai", "日高" "Hidaka", "山川" "Yamakawa", "藤川" "Fujikawa", "松原" "Matsubara", "浜崎" "Hamasaki", "岡本" "Okamoto", "丸山" "Maruyama", "松島" "Matsushima", "河村" "Koumura", "岡部" "Okabe", "土井" "Doi", "古田" "Furuta", "松下" "Matsushita", "松田" "Matsuda", "竹本" "Takemoto", "中西" "Nakanishi", "中野" "Nakano", "岡田" "Okada", "前川" "Maekawa", "原" "Hara", "石山" "Ishiyama", "吉本" "Yoshimoto", "久保" "Kubo", "長島" "Nagashima", "北野" "Kitano", "堀江" "Horie", "大原" "Oohara", "茂木" "Moteki", "杉山" "Sugiyama", "三輪" "Miwa", "尾崎" "Ozaki", "石田" "Ishida", "今井" "Imai", "川原" "Kawahara", "古川" "Furukawa", "長岡" "Nagaoka", "徳永" "Tokunaga", "臼井" "Usui", "竹田" "Chikuda", "奥野" "Okuno", "原田" "Harada", "武藤" "Mutou", "中原" "Nakahara", "本田" "Honda", "白石" "Shiraishi", "山村" "Yamamura", "小笠原" "Ogasahara", "小出" "Koide", "瀬戸" "Seto", "井上" "Inoue", "川野" "Kouno", "根岸" "Negishi", "細川" "Hosokawa", "篠田" "Shinoda", "藤原" "Fujihara", "奥田" "Okuta", "高野" "Takano", "西尾" "Nishio", "飯島" "Iijima", "酒井" "Sakei", "宮川" "Miyakawa", "入江" "Irie", "下村" "Shitamura", "内藤" "Naitou", "森本" "Morimoto", "榊原" "Sakakihara", "堀口" "Horiguchi", "浜野" "Hamano", "沢田" "Sawada", "富田" "Tomita", "越智" "Kochi", "北川" "Kitakawa", "鎌田" "Kamuta", "佐伯" "Sahaku", "中嶋" "Nakashima", "西村" "Nishimura", "片桐" "Katagiri", "萩原" "Hagiwara", "稲葉" "Inaba", "谷" "Hazama", "浅田" "Asada", "谷本" "Tanimoto", "平井" "Hirai", "山田" "Yamada", "上野" "Ueno", "倉田" "Kurata", "内田" "Uchida", "三宅" "Miyake", "飯塚" "Meshizuka", "緒方" "Ogata", "望月" "Motsuzuki", "田畑" "Tabata", "田村" "Tamura", "小谷" "Kotani", "小山" "Koyama", "川端" "Kawabata", "小島" "Kojima", "上原" "Kamihara", "足立" "Adachi", "古谷" "Furutani", "坂田" "Sakata", "本多" "Honda", "秋元" "Akimoto", "沼田" "Numata", "山岡" "Yamaoka", "石川" "Ishikawa", "塚田" "Tsukada", "高橋" "Takahashi", "江口" "Eguchi", "林田" "Hayashida", "豊田" "Toyota", "大場" "Ooba", "工藤" "Kodou", "川田" "Kawata", "市川" "Ichikawa", "奥山" "Okuyama", "浜口" "Hamaguchi", "福永" "Fukunaga", "千田" "Chida", "園田" "Sonoda", "高山" "Takayama", "笠井" "Kasai", "青木" "Aoki", "宮内" "Miyauchi", "佐久間" "Sakuma", "馬場" "Baba", "藤沢" "Fujisawa", "森田" "Morita", "片山" "Katayama", "堀内" "Horiuchi", "大村" "Oomura", "吉岡" "Yoshioka", "荒川" "Arakawa", "堀川" "Horikawa", "竹内" "Takeuchi", "篠原" "Shinohara", "南" "Minami", "藤岡" "Fujioka", "小野" "Sanu", "甲斐" "Kai", "田代" "Tashiro", "大沢" "Oosawa", "大石" "Ooishi", "西田" "Nishida", "高木" "Takaki", "牧野" "Makino", "後藤" "Gotou", "筒井" "Tsutsui", "谷口" "Taniguchi", "平山" "Hirayama", "堤" "Tsutsumi", "佐藤" "Satou", "吉川" "Yoshikawa", "福田" "Fukuda", "大内" "Oouchi", "長沢" "Nagasawa", "黒沢" "Kurosawa", "大橋" "Oohashi", "渋谷" "Shibutani", "渡部" "Watanabe", "栗田" "Kurita", "岩瀬" "Iwase", "畑中" "Hatakenaka", "岩田" "Iwata", "三谷" "Mitani", "岩本" "Iwamoto", "川崎" "Kawasaki", "三上" "Mikami", "森下" "Morishita", "小野寺" "Onodera", "小池" "Koike", "藤田" "Fujita", "加藤" "Katou", "島崎" "Shimasaki", "矢野" "Yano", "中井" "Nakai", "関根" "Sekine", "神田" "Kouda", "土屋" "Tsuchiya", "桑原" "Kuwabara", "柳沢" "Yanagisawa", "森山" "Moriyama", "古賀" "Koga", "町田" "Machida", "中尾" "Nakao", "松崎" "Matsusaki", "川上" "Kawakami", "渡辺" "Watanabe", "中谷" "Nakatani", "西原" "Nishihara", "前田" "Maeda", "大竹" "Ootake", "河原" "Gouhara", "菊池" "Kikuchi", "宮下" "Miyashita", "山根" "Yamane", "青柳" "Aoyagi", "徳田" "Tokuta", "高島" "Takashima", "西" "Nishi", "菊地" "Kikuchi", "松永" "Matsunaga", "山中" "Yamanaka", "冨田" "Tomita", "下田" "Shimoda", "横井" "Yokoi", "野沢" "Nozawa", "藤野" "Fujino", "中本" "Nakamoto", "田口" "Taguchi", "村田" "Murata", "村井" "Murai", "八木" "Yagi", "小坂" "Kosaka", "森川" "Morikawa", "黒田" "Kurota", "鶴田" "Tsuruta", "小柳" "Koyanagi", "富永" "Tominaga", "相馬" "Souma", "小林" "Kobayashi", "小倉" "Kokura", "太田" "Oota", "柏木" "Hakugi", "北原" "Kitahara", "中山" "Nakayama", "西山" "Nishiyama", "近藤" "Chikafuji", "原口" "Haraguchi", "杉原" "Sugihara", "天野" "Tenno", "金子" "Kaneko", "大城" "Daijou", "長田" "Nagata", "佐々木" "Sasaki", "中島" "Nakashima", "小沢" "Kozawa", "松岡" "Matsuoka", "金井" "Kanai", "西川" "Nishikawa", "北村" "Kitamura", "吉田" "Yoshida", "田辺" "Tanabe", "桜井" "Sakurai", "戸田" "Toda", "野中" "Nonaka", "及川" "Shikikawa", "阿部" "Abe", "片岡" "Kataoka", "田島" "Tashima", "大田" "Daita", "内山" "Uchiyama", "早川" "Hayakawa", "井口" "Iguchi", "手塚" "Tezuka", "米田" "Yoneda", "金沢" "Kanazawa", "二宮" "Nimiya", "福原" "Fukuhara", "菅野" "Sugano", "川本" "Kawamoto", "安藤" "Andou", "大川" "Daikawa", "辻" "Tsuji", "安達" "Adachi", "根本" "Memoto", "松村" "Matsumura", "岡村" "Okamura", "宮崎" "Miyazaki", "藤村" "Fujimura", "松井" "Matsui", "黒木" "Kuroki", "新井" "Nii", "比嘉" "Hiyoshi", "野田" "Noda", "五十嵐" "Ikarashi", "小田" "Koda", "石黒" "Ishikuno", "塚本" "Tsukamoto", "谷川" "Yagawa", "竹中" "Takenaka", "土田" "Toda", "千葉" "Chiba", "菅" "Suga", "大崎" "Daisaki", "高田" "Takata", "橋本" "Hashimoto", "久保田" "Kubota", "横山" "Yokoyama", "関口" "Sekiguchi", "大木" "Ooki", "竹下" "Takeshita", "内海" "Utsumi", "北島" "Kitajima", "篠崎" "Shinosaki", "志村" "Shimura", "秋山" "Akiyama", "山本" "Yamamoto", "石塚" "Ishizuka", "平野" "Hirano", "坂井" "Sakai", "野村" "Nomura", "松野" "Matsuno", "長谷川" "Hayagawa", "三木" "Miki", "大谷" "Ootani", "日野" "Hino", "須藤" "Sutou", "榎本" "Enomoto", "田原" "Tawara", "清水" "Shimizu", "松山" "Matsuyama", "永田" "Nagata", "斉藤" "Saitou", "森岡" "Morioka", "植田" "Ueda", "平岡" "Hiraoka", "福岡" "Fukuoka", "小西" "Konishi", "平川" "Hirakawa", "石井" "Ishii", "浜田" "Hamada", "荒木" "Araki", "柳田" "Yanagita", "中川" "Nakagawa", "石原" "Ishihara", "大森" "Oomori", "中村" "Nakamura", "吉沢" "Yoshizawa", "河合" "Kawai", "上田" "Jouda", "安田" "Yasuda", "杉本" "Sugimoto", "吉野" "Yoshino", "永野" "Nagano", "山岸" "Yamagishi", "秋田" "Akita", "野崎" "Nozaki", "西沢" "Nishizawa", "服部" "Hatsutori", "広田" "Hirota", "金田" "Kaneda", "宮沢" "Miyazawa", "深沢" "Fukasawa", "松浦" "Matsuura", "伊藤" "Itou", "荒井" "Arai", "石橋" "Ishibashi", "笠原" "Kasahara", "村瀬" "Murase", "宇野" "Uno", "中田" "Nakada", "村山" "Murayama", "宮原" "Miyabara", "福井" "Fukui", "岸本" "Kishimoto", "坂本" "Sakamoto", "大平" "Taihei", "星" "Hoshi", "池上" "Ikegami", "溝口" "Mizokuchi", "庄司" "Shiyouji", "福本" "Fukumoto", "宮田" "Miyata", "堀田" "Hotsuda", "武田" "Takeda", "多田" "Tada", "小泉" "Koizumi", "泉" "Izumi", "堀" "Hori", "荻野" "Ogino", "藤井" "Fujii", "平松" "Hiramatsu", "亀井" "Kamei", "松本" "Matsumoto", "宮本" "Miyamoto", "若林" "Wakabayashi", "村上" "Murakami", "山下" "Yamashita", "湯浅" "Yuasa", "山内" "Yamauchi", "熊谷" "Kumatani", "高松" "Takamatsu", "須田" "Suda", "和田" "Wada", "白川" "Shirakawa", "池田" "Ikeda", "杉浦" "Sugiura", "吉原" "Yoshihara", "岡崎" "Okazaki", "坂口" "Sakaguchi", "奥村" "Okumura", "杉田" "Sugita", "浅野" "Asano", "西野" "Nishino", "水野" "Mizuno", "平田" "Heida", "伊東" "Itou", "福島" "Fukushima", "大塚" "Ootsuka", "田上" "Tagami", "金城" "Kinjou", "飯田" "Iida", "柴田" "Shibata", "高井" "Takai", "大山" "Daisen", "水谷" "Mizutani", "松尾" "Matsuo", "畠山" "Hatakeyama", "斎藤" "Saitou", "中沢" "Nakazawa", "林" "Rin", "浅井" "Asai", "梅田" "Umeta", "小森" "Komori", "田中" "Tanaka", "藤本" "Fujimoto", "鈴木" "Suzuki", "菅原" "Sugahara", "木下" "Kishita", "横田" "Yokota", "神谷" "Shintani", "本間" "Honma", "大槻" "Ootsuki", "加納" "Kanou", "武井" "Takei", "竹村" "Takemura", "嶋田" "Shimada", "栗原" "Kurihara", "今村" "Imamura", "大西" "Oonishi", "安井" "Yasui", "児玉" "Kodama", "出口" "Deguchi", "星野" "Hoshino", "西岡" "Nishioka", "吉村" "Yoshimura", "吉井" "Yoshii", "増田" "Masuda", "三浦" "Miura", "岩崎" "Iwasaki", "津田" "Tsuda", "樋口" "Higuchi", "森" "Mori", "山崎" "Yamasaki", "寺田" "Terada", "安部" "Abe", "黒川" "Kurokawa", "角田" "Shinoda", "新田" "Niita", "花田" "Hanada", "石崎" "Ishizaki", "小川" "Kokawa", "小松" "Komatsu", "山口" "Yamaguchi", "青山" "Aoyama", "稲垣" "Inagaki", "長尾" "Nagao", "今野" "Konno", "滝沢" "Takisawa"})

(def japanese-given-names-and-romaji
  {"優太" "Yuuta", "海斗" "Kaito", "千代子" "Chiyoko", "夏希" "Natsuki", "大地" "Daichi", "優奈" "Yuuna", "大雅" "Taiga", "悠真" "Yuuma", "湊" "Minato", "由美" "Yumi", "恵" "Megumi", "健太" "Kenta", "美菜" "Mina", "美優" "Miyu", "清" "Kiyoshi", "麗華" "Reika", "結衣" "Yui", "芽依" "Mei", "克己" "Katsumi", "愛梨" "Airi", "優香" "Yuuka", "隼人" "Hayato", "勇" "Isamu", "直樹" "Naoki", "愛美" "Emi", "大輔" "Daisuke", "悠人" "Yuto", "哲也" "Tetsuya", "貞子" "Teiko", "遥" "Haruka", "紅子" "Beniko", "誠" "Makoto", "颯太" "Souta", "結菜" "Yuuna", "正男" "Masao", "大和" "Yamato", "優月" "Yuzuki", "寛" "Kan", "真一" "Shinichi", "彩美" "Ayami", "緑" "Midori", "陽子" "Youko", "正" "Tadashi", "恵介" "Keisuke", "和子" "Kazuko", "静香" "Shizuka", "良子" "Yoshiko", "恵美" "Emi", "千代" "Chiyo", "愛菜" "Mana", "梅子" "Umeko", "陸" "Riku", "蒼空" "Sora", "桐子" "Kiriko", "学" "Manabu", "萌" "Moe", "樹" "Itsuki", "四郎" "Shiro", "勝" "Masaru", "駿" "Shun", "由美子" "Yumiko", "朋美" "Tomomi", "螢" "Hotaru", "幸子" "Sachiko", "法子" "Noriko", "正博" "Masahiro", "皐" "Satsuki", "大輝" "Taiki", "華" "Hana", "節子" "Setsuko", "延" "Nobu", "智子" "Tomoko", "愛子" "Aiko", "真" "Shin", "和美" "Kazumi", "彩花" "Ayaka", "康平" "Kouhei", "瞳" "Hitomi", "虎太郎" "Kotarou", "重子" "Shigeko", "凛" "Rin", "桃子" "Momoko", "霞" "Kasumi", "弘子" "Hiroko", "龍之介" "Ryunosuke", "和也" "Kazuya", "孝" "Takashi", "雪子" "Yukiko", "小百合" "Sayuri", "愛理" "Airi", "匠" "Takumi", "進" "Susumu", "翼" "Tsubasa", "文子" "Fumiko", "真弓" "Mayumi", "慶子" "Keiko", "涼" "Ryo", "百合子" "Yuriko", "豊" "Yutaka", "健三" "Kenzo", "修" "Osamu", "真美" "Mami", "明美" "Akemi", "茂" "Shigeru", "杏那" "Anna", "実" "Minoru", "心優" "Miyu", "葵" "Aoi", "一樹" "Kazuki", "直美" "Naomi", "丈夫" "Takeo", "翔太" "Shouta", "大翔" "Hiroto", "優斗" "Yuto", "陽斗" "Haruto", "舞" "Mai", "心春" "Koharu", "剛" "Tsuyoshi", "瑛太" "Eita", "七海" "Nanami", "春菜" "Haruna", "雄大" "Yuudai", "雨夜" "Amaya", "莉子" "Riko", "蓮" "Ren", "禎子" "Sadako", "翔" "Shou", "美咲" "Misaki", "千夏" "Chinatsu", "昭夫" "Akio", "義雄" "Yoshio", "拓也" "Takuya", "悠斗" "Yuto", "美羽" "Miu", "三郎" "Saburo", "陽菜" "Hina", "陽翔" "Haruto", "武" "Takeshi", "愛" "Ai", "桜" "Sakura", "彩乃" "Ayano", "香" "Kaori", "久美子" "Kumiko", "咲希" "Saki", "颯真" "Souma", "直子" "Naoko", "大樹" "Hiroki", "結愛" "Yua", "明" "Akira"})

(def gen-japanese-surnames
  (gen/elements (vec (keys japanese-surnames-and-romaji))))

(def gen-japanese-given-names
  (gen/elements (vec (keys japanese-given-names-and-romaji))))

(def gen-japanese-names
  (gen/tuple gen-japanese-surnames gen-japanese-given-names))

(def english-surnames
  ["Smith" "Jones" "Williams" "Taylor" "Brown" "Davies" "Evans" "Wilson" "Thomas" "Johnson" "Roberts" "Robinson" "Thompson" "Wright" "Walker" "White" "Edwards" "Hughes" "Green" "Hall" "Lewis" "Harris" "Clarke" "Patel" "Jackson" "Wood" "Turner" "Martin" "Cooper" "Hill" "Ward" "Morris" "Moore" "Clark" "Lee" "King" "Baker" "Harrison" "Morgan" "Allen" "James" "Scott" "Phillips" "Watson" "Davis" "Parker" "Price" "Bennett" "Young" "Griffiths" "Mitchell" "Kelly" "Cook" "Carter" "Richardson" "Bailey" "Collins" "Bell" "Shaw" "Murphy" "Miller" "Cox" "Richards" "Khan" "Marshall" "Anderson" "Simpson" "Ellis" "Adams" "Singh" "Begum" "Wilkinson" "Foster" "Chapman" "Powell" "Webb" "Rogers" "Gray" "Mason" "Ali" "Hunt" "Hussain" "Campbell" "Matthews" "Owen" "Palmer" "Holmes" "Mills" "Barnes" "Knight" "Lloyd" "Butler" "Russell" "Barker" "Fisher" "Stevens" "Jenkins" "Murray" "Dixon" "Harvey" "Graham" "Pearson" "Ahmed" "Fletcher" "Walsh" "Kaur" "Gibson" "Howard" "Andrews" "Stewart" "Elliott" "Reynolds" "Saunders" "Payne" "Fox" "Ford" "Pearce" "Day" "Brooks" "West" "Lawrence" "Cole" "Atkinson" "Bradley" "Spencer" "Gill" "Dawson" "Ball" "Burton" "O" "Watts" "Rose" "Booth" "Perry" "Ryan" "Grant" "Wells" "Armstrong" "Francis" "Rees" "Hayes" "Hart" "Hudson" "Newman" "Barrett" "Webster" "Hunter" "Gregory" "Carr" "Lowe" "Page" "Marsh" "Riley" "Dunn" "Woods" "Parsons" "Berry" "Stone" "Reid" "Holland" "Hawkins" "Harding" "Porter" "Robertson" "Newton" "Oliver" "Reed" "Kennedy" "Williamson" "Bird" "Gardner" "Shah" "Dean" "Lane" "Cooke" "Bates" "Henderson" "Parry" "Burgess" "Bishop" "Walton" "Burns" "Nicholson" "Shepherd" "Ross" "Cross" "Long" "Freeman" "Warren" "Nicholls" "Hamilton" "Byrne" "Sutton" "Mcdonald" "Yates" "Hodgson" "Robson" "Curtis" "Hopkins" "O" "Harper" "Coleman" "Watkins" "Moss" "Mccarthy" "Chambers" "O" "Griffin" "Sharp" "Hardy" "Wheeler" "Potter" "Osborne" "Johnston" "Gordon" "Doyle" "Wallace" "George" "Jordan" "Hutchinson" "Rowe" "Burke" "May" "Pritchard" "Gilbert" "Willis" "Higgins" "Read" "Miles" "Stevenson" "Stephenson" "Hammond" "Arnold" "Buckley" "Walters" "Hewitt" "Barber" "Nelson" "Slater" "Austin" "Sullivan" "Whitehead" "Mann" "Frost" "Lambert" "Stephens" "Blake" "Akhtar" "Lynch" "Goodwin" "Barton" "Woodward" "Thomson" "Cunningham" "Quinn" "Barnett" "Baxter" "Bibi" "Clayton" "Nash" "Greenwood" "Jennings" "Holt" "Kemp" "Poole" "Gallagher" "Bond" "Stokes" "Tucker" "Davidson" "Fowler" "Heath" "Norman" "Middleton" "Lawson" "Banks" "French" "Stanley" "Jarvis" "Gibbs" "Ferguson" "Hayward" "Carroll" "Douglas" "Dickinson" "Todd" "Barlow" "Peters" "Lucas" "Knowles" "Hartley" "Miah" "Simmons" "Morton" "Alexander" "Field" "Morrison" "Norris" "Townsend" "Preston" "Hancock" "Thornton" "Baldwin" "Burrows" "Briggs" "Parkinson" "Reeves" "Macdonald" "Lamb" "Black" "Abbott" "Sanders" "Thorpe" "Holden" "Tomlinson" "Perkins" "Ashton" "Rhodes" "Fuller" "Howe" "Bryant" "Vaughan" "Dale" "Davey" "Weston" "Bartlett" "Whittaker" "Davison" "Kent" "Skinner" "Birch" "Morley" "Daniels" "Glover" "Howell" "Cartwright" "Pugh" "Humphreys" "Goddard" "Brennan" "Wall" "Kirby" "Bowen" "Savage" "Bull" "Wong" "Dobson" "Smart" "Wilkins" "Kirk" "Fraser" "Duffy" "Hicks" "Patterson" "Bradshaw" "Little" "Archer" "Warner" "Waters" "O" "Farrell" "Brookes" "Atkins" "Kay" "Dodd" "Bentley" "Flynn" "John" "Schofield" "Short" "Haynes" "Wade" "Butcher" "Henry" "Sanderson" "Crawford" "Sheppard" "Bolton" "Coates" "Giles" "Gould" "Houghton" "Gibbons" "Pratt" "Manning" "Law" "Hooper" "Noble" "Dyer" "Rahman" "Clements" "Moran" "Sykes" "Chan" "Doherty" "Connolly" "Joyce" "Franklin" "Hobbs" "Coles" "Herbert" "Steele" "Kerr" "Leach" "Winter" "Owens" "Duncan" "Naylor" "Fleming" "Horton" "Finch" "Fitzgerald" "Randall" "Carpenter" "Marsden" "Browne" "Garner" "Pickering" "Hale" "Dennis" "Vincent" "Chadwick" "Chandler" "Sharpe" "Nolan" "Lyons" "Hurst" "Collier" "Peacock" "Howarth" "Faulkner" "Rice" "Pollard" "Welch" "Norton" "Gough" "Sinclair" "Blackburn" "Bryan" "Conway" "Power" "Cameron" "Daly" "Allan" "Hanson" "Gardiner" "Boyle" "Myers" "Turnbull" "Wallis" "Mahmood" "Sims" "Swift" "Iqbal" "Pope" "Brady" "Chamberlain" "Rowley" "Tyler" "Farmer" "Metcalfe" "Hilton" "Godfrey" "Holloway" "Parkin" "Bray" "Talbot" "Donnelly" "Nixon" "Charlton" "Benson" "Whitehouse" "Barry" "Hope" "Lord" "North" "Storey" "Connor" "Potts" "Bevan" "Hargreaves" "Mclean" "Mistry" "Bruce" "Howells" "Hyde" "Parkes" "Wyatt" "Fry" "Lees" "O'donnel" "Craig" "Forster" "Mckenzie" "Humphries" "Mellor" "Carey" "Ingram" "Summers" "Leonard"])

(def english-male-names
  ["James" "John" "Robert" "Michael" "William" "David" "Richard" "Joseph" "Charles" "Thomas" "Christopher" "Daniel" "Matthew" "Donald" "Anthony" "Mark" "Paul" "Steven" "George" "Kenneth" "Andrew" "Edward" "Joshua" "Brian" "Kevin" "Ronald" "Timothy" "Jason" "Jeffrey" "Ryan" "Gary" "Nicholas" "Eric" "Jacob" "Stephen" "Jonathan" "Larry" "Frank" "Scott" "Justin" "Brandon" "Raymond" "Gregory" "Samuel" "Benjamin" "Patrick" "Jack" "Dennis" "Alexander" "Jerry" "Tyler" "Henry" "Douglas" "Aaron" "Peter" "Jose" "Walter" "Adam" "Zachary" "Nathan" "Harold" "Kyle" "Carl" "Arthur" "Gerald" "Roger" "Keith" "Lawrence" "Jeremy" "Terry" "Albert" "Joe" "Sean" "Willie" "Christian" "Jesse" "Austin" "Billy" "Bruce" "Ralph" "Bryan" "Ethan" "Roy" "Eugene" "Jordan" "Louis" "Wayne" "Alan" "Harry" "Russell" "Juan" "Dylan" "Randy" "Philip" "Vincent" "Noah" "Bobby" "Howard" "Gabriel" "Johnny"])

(def english-female-names
  ["Mary" "Patricia" "Jennifer" "Elizabeth" "Linda" "Barbara" "Susan" "Margaret" "Jessica" "Sarah" "Dorothy" "Karen" "Nancy" "Betty" "Lisa" "Sandra" "Ashley" "Kimberly" "Donna" "Helen" "Carol" "Michelle" "Emily" "Amanda" "Melissa" "Deborah" "Laura" "Stephanie" "Rebecca" "Sharon" "Cynthia" "Kathleen" "Anna" "Shirley" "Ruth" "Amy" "Angela" "Brenda" "Virginia" "Pamela" "Catherine" "Katherine" "Nicole" "Christine" "Samantha" "Janet" "Debra" "Carolyn" "Rachel" "Heather" "Maria" "Diane" "Julie" "Joyce" "Emma" "Frances" "Evelyn" "Joan" "Martha" "Christina" "Kelly" "Lauren" "Victoria" "Judith" "Alice" "Ann" "Cheryl" "Jean" "Doris" "Megan" "Marie" "Andrea" "Kathryn" "Jacqueline" "Gloria" "Teresa" "Sara" "Janice" "Hannah" "Julia" "Rose" "Theresa" "Grace" "Judy" "Beverly" "Olivia" "Denise" "Marilyn" "Amber" "Danielle" "Brittany" "Diana" "Mildred" "Jane" "Madison" "Lori" "Tiffany" "Kathy" "Tammy" "Kayla"])

(def gen-english-surname
  (gen/elements english-surnames))

(def gen-english-male-names
  (gen/elements english-male-names))

(def gen-english-female-names
  (gen/elements english-female-names))

(def gen-english-given-names
  (gen/elements (into english-male-names english-female-names)))

(def gen-english-names
  (gen/tuple gen-english-surname gen-english-given-names))

(def gen-all-names
  (gen/frequency [[5 gen-english-names] [5 gen-japanese-names]]))

(def gen-all-given-names
  (gen/frequency [[5 gen-english-given-names] [5 gen-japanese-given-names]]))

(def gen-all-surnames
  (gen/frequency [[5 gen-english-surname] [5 gen-japanese-surnames]]))

(def gen-msg-body
  (gen/not-empty gen/string-alphanumeric))

(defn gen-msg [user channel]
  (gen/fmap (fn [[body]]
              (let [db-id (d/tempid :db.part/user)]
                {:db/id         db-id
                 :dato/guid     (d/squuid)
                 :dato/type     (gen/return :kandan.type/msg)
                 :msg/user      (:db/id user)
                 :msg/body      body
                 :msg/at        (now)
                 :channel/_msgs [{:db/id db-id}]}))
            (gen/tuple gen-msg-body)))

(defn make-user-gen [org]
  (gen/hash-map
   :db/id            gen-datomic-ids
   :dato/guid        gen-dato-guids
   :dato/type        (gen/return :kandan.type/user)
   :user/disabled?   gen/boolean
   :user/email       (make-long-string-gen 10)
   :user/nick        (make-long-string-gen 3)
   :user/given-name  gen-all-given-names
   :user/family-name gen-all-surnames))

(defn make-channel-gen [org users]
  (gen/hash-map
   :db/id              gen-datomic-ids
   :dato/guid          gen-dato-guids
   :dato/type          (gen/return :kandan.type/channel)
   :channel/title      (make-long-string-gen 6)
   :channel/topic      (make-long-string-gen 20)
   :channel/created-at gen-inst
   :channel/admins     (gen/elements users)
   :channel/members    (gen/return users)))

(def gen-org
  (let [org-eid  (d/tempid :db.part/user)
        users    (gen/sample (make-user-gen {:db/id org-eid}) 10)
        channels (gen/sample (make-channel-gen {:db/id org-eid} users) 10)]
    (gen/fmap
     (fn [[name]]
       {:db/id        org-eid
        :dato/guid    (d/squuid)
        :dato/type    (gen/return :kandan.type/org)
        :org/name     name
        :org/users    users
        :org/admins   (take 5 users)
        :org/channels channels})
     (gen/tuple (make-long-string-gen 6)))))

(defn basic-datoms []
  (let [org-eid        (d/tempid :db.part/user)
        first-user-eid (d/tempid :db.part/user)
        users          [{:db/id            first-user-eid
                         :dato/guid        (d/squuid)
                         :dato/type        :kandan.type/user
                         :user/email       "dww@gmail.com"
                         :user/nick        "dww"
                         :user/given-name  "Daniel"
                         :user/family-name "Woelfel"}
                        {:db/id            (d/tempid :db.part/user)
                         :dato/guid        (d/squuid)
                         :dato/type        :kandan.type/user
                         :user/email       "s@bushi.do"
                         :user/nick        "sgrove"
                         :user/given-name  "Sean"
                         :user/family-name "Grove"}]
        channels       [{:db/id              (d/tempid :db.part/user)
                         :dato/guid          (d/squuid)
                         :dato/type          :kandan.type/channel
                         :channel/title      "Home"
                         :channel/topic      "Welcome home"
                         :channel/created-at (now)
                         :channel/admins     users
                         :channel/members    users
                         :channel/msgs       [{:db/id       (d/tempid :db.part/user)
                                               :dato/guid   (d/squuid)
                                               :dato/type :kandan.type/msg
                                               :msg/user    {:db/id first-user-eid}
                                               :msg/body    "First message ever in home"
                                               :msg/pinned? true}
                                              {:db/id     (d/tempid :db.part/user)
                                               :dato/guid (d/squuid)
                                               :dato/type :kandan.type/msg
                                               :msg/user  {:db/id first-user-eid}
                                               :msg/body  "We welcome you here, to home"}]}]
        org            {:db/id        org-eid
                        :dato/guid    (d/squuid)
                        :dato/type    :kandan.type/org
                        :org/name     "Bushido"
                        :org/users    users
                        :org/admins   [(first users)]
                        :org/channels channels}]
    org))

(defn reseed-db! [conn]
  (let [basic (basic-datoms)
        orgs  (gen/sample gen-org 3)]
    (d/transact conn (conj [] basic))))
