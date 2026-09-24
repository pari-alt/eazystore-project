import { createSlice } from "@reduxjs/toolkit";

const getStoredUser = () => {
  try {
    const user = localStorage.getItem("user");
    return user ? JSON.parse(user) : null;
  } catch {
    localStorage.removeItem("user");
    return null;
  }
};

const storedToken = localStorage.getItem("jwtToken");
const storedUser = getStoredUser();

const initialAuthState = {
  jwtToken: storedToken || null,
  user: storedUser,
  isAuthenticated: !!(storedToken && storedUser),
};

const authSlice = createSlice({
  name: "auth",
  initialState: initialAuthState,

  reducers: {
    loginSuccess(state, action) {
      const { jwtToken, user } = action.payload;

      state.jwtToken = jwtToken;
      state.user = user;
      state.isAuthenticated = !!(jwtToken && user);

      if (jwtToken && user) {
        localStorage.setItem("jwtToken", jwtToken);
        localStorage.setItem("user", JSON.stringify(user));
      }
    },

    logout(state) {
      state.jwtToken = null;
      state.user = null;
      state.isAuthenticated = false;

      localStorage.removeItem("jwtToken");
      localStorage.removeItem("user");
    },
  },
});

export const { loginSuccess, logout } = authSlice.actions;
export default authSlice.reducer;

export const selectJwtToken = (state) => state.auth.jwtToken;
export const selectUser = (state) => state.auth.user;
export const selectIsAuthenticated = (state) =>
  state.auth.isAuthenticated;