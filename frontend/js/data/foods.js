const imageIds = [
	"0fa47306-aff1-4c57-b429-216b9e8407b4", "4360301e-fdba-4722-84fa-df1e0c6747de",
	"9ed777fc-0c09-4ea8-8be4-6b9a41873c60", "da19eca9-1402-4992-a631-06248b0fb439",
	"7bdfaf38-4d1d-41a2-8274-67df36c42d3f", "64372adc-de9a-4686-8ef9-e34e7947d594",
	"aa033c0d-98e0-475c-9eeb-968acec4a8bc", "c1d2e10b-69d7-4084-b5e8-5c36a0dd20a9",
	"ae94c995-1a07-4209-a5f3-93246fa0b462", "7f85c62f-fa58-4738-a588-cf8db5be2e19",
	"ef21b908-62e8-4293-b75c-6afc58f474e1", "bb464075-6ae4-4753-9106-8db0c91b94d2",
	"3e06abcc-b9c5-479b-a44f-234f2ac6ee7e", "b134572f-115d-43e2-bfa2-bbb6a80b7233",
	"da25bb49-7530-4485-8d2a-5b5f246ca486", "e1281dc8-d456-4f80-8cd2-4da2d204b2f6",
	"014fe67b-34dd-403d-99aa-fe77a91c404f", "1d91a81f-7751-485f-9b9e-741ecdfc2e61",
	"22e788bf-65ec-4766-bf9c-b7b54c715d76", "24ab84c0-1bf4-41ec-a57d-ce2cce875fa4",
	"2b0f04be-d3b3-4f9a-920d-af3067d769f5", "35fd2795-7605-4d62-b489-3dab05cd218a",
	"3bff889a-e324-4218-9259-2d8a311c9380", "42711178-c61f-49c4-8423-65d5e1be153c",
	"49fdc2a3-d2bb-498a-8351-43af316ab6f0"
];

const photo = (index) => `images/${imageIds[index % imageIds.length]}.png`;

const foods = [
	{ name: "Phở gà ức bỏ da nước trong", calories: 420, protein: 38, carbs: 52, fat: 12, image: 9 },
	{ name: "Salad ức gà mè rang", calories: 380, protein: 34, carbs: 28, fat: 16, image: 3 },
	{ name: "Cá hồi áp chảo sốt bơ chanh", calories: 480, protein: 42, carbs: 22, fat: 18, image: 11 },
	{ name: "Cơm tấm sườn nạc", calories: 620, protein: 32, carbs: 78, fat: 18, image: 10 },
	{ name: "Bún bò Huế", calories: 520, protein: 30, carbs: 58, fat: 16, image: 12 },
	{ name: "Gỏi cuốn tôm thịt", calories: 180, protein: 12, carbs: 22, fat: 4, image: 13 }
];
