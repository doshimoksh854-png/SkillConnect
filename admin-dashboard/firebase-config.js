// Firebase Config for SkillConnect Admin Dashboard
const firebaseConfig = {
    apiKey: "AIzaSyBfLkFYQRoIrTp-8qpGCmyJXrrZDaBQ8Io",
    authDomain: "skillconnect-58ce2.firebaseapp.com",
    projectId: "skillconnect-58ce2",
    storageBucket: "skillconnect-58ce2.firebasestorage.app",
    messagingSenderId: "884997675425",
    appId: "1:884997675425:web:7aa9af980e9c3dfb3653e0"
};

firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const db = firebase.firestore();
