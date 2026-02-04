import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  reactCompiler: true,
  output: 'standalone', // ✅ Requis pour Docker deployment
};

export default nextConfig;
