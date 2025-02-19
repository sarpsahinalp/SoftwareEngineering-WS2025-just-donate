import { NavBar } from '../../components/organization/NavigationBar';
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Donation Platform',
  description: '"Manage·your·organization\'s·donations·and·transactions"',
};

export default function OrganizationLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className={inter.className}>
      <NavBar />
      <main className="container mx-auto p-4">{children}</main>
    </div>
  );
}
